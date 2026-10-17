package com.canvaskanban.dao;

import com.canvaskanban.database.Database;
import com.canvaskanban.model.Priority;
import com.canvaskanban.model.Task;
import com.canvaskanban.model.TaskStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes tasks, including moving them between the board columns.
 *
 * <p>The {@code position} column stores the order of the cards inside one column.
 * Whenever a card is moved, the positions of that column are rewritten from 0 upwards
 * inside a single transaction, so the database can never contain a half finished move.</p>
 */
public class TaskDao {

    private static final String SELECT_COLUMNS =
            "SELECT id, board_id, title, description, priority, status, position, created_at, due_date FROM tasks";

    private final Database database;

    public TaskDao(Database database) {
        this.database = database;
    }

    /** Saves a new task at the end of its column and copies the generated id into it. */
    public Task insert(Task task) {
        String sql = "INSERT INTO tasks (board_id, title, description, priority, status, position, created_at, due_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection()) {
            int position = task.getPosition() > 0
                    ? task.getPosition()
                    : nextPosition(connection, task.getBoardId(), task.getStatus());
            LocalDate createdAt = task.getCreatedAt() == null ? LocalDate.now() : task.getCreatedAt();

            try (PreparedStatement statement =
                         connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, task.getBoardId());
                statement.setString(2, task.getTitle());
                statement.setString(3, task.getDescription());
                statement.setString(4, task.getPriority().name());
                statement.setString(5, task.getStatus().name());
                statement.setInt(6, position);
                statement.setString(7, createdAt.toString());
                statement.setString(8, SqlDates.toText(task.getDueDate()));
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        task.setId(keys.getLong(1));
                    }
                }
            }
            task.setPosition(position);
            task.setCreatedAt(createdAt);
            return task;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the task \"" + task.getTitle() + "\".", e);
        }
    }

    /** Every task of a board, grouped by column and ordered inside each column. */
    public List<Task> findByBoard(long boardId) {
        String sql = SELECT_COLUMNS + " WHERE board_id = ? ORDER BY status, position, id";
        List<Task> tasks = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, boardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tasks.add(read(resultSet));
                }
            }
            return tasks;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the tasks of board " + boardId + ".", e);
        }
    }

    /** The tasks of one column, in the order they are displayed. */
    public List<Task> findByBoardAndStatus(long boardId, TaskStatus status) {
        String sql = SELECT_COLUMNS + " WHERE board_id = ? AND status = ? ORDER BY position, id";
        List<Task> tasks = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, boardId);
            statement.setString(2, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tasks.add(read(resultSet));
                }
            }
            return tasks;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the tasks of board " + boardId + ".", e);
        }
    }

    public Optional<Task> findById(long id) {
        try (Connection connection = database.getConnection()) {
            return findById(connection, id);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load task " + id + ".", e);
        }
    }

    /**
     * Updates the content of a task (title, description, priority and due date).
     * The column and the order are changed with {@link #moveTask}.
     */
    public boolean updateDetails(Task task) {
        String sql = "UPDATE tasks SET title = ?, description = ?, priority = ?, due_date = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, task.getTitle());
            statement.setString(2, task.getDescription());
            statement.setString(3, task.getPriority().name());
            statement.setString(4, SqlDates.toText(task.getDueDate()));
            statement.setLong(5, task.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the task \"" + task.getTitle() + "\".", e);
        }
    }

    /**
     * Shortcut used by the tests and by simple status changes: puts the task at the
     * end of the given column.
     */
    public boolean updateStatus(long taskId, TaskStatus status) {
        return moveTask(taskId, status, Integer.MAX_VALUE);
    }

    /**
     * Moves a task to another column and/or another place inside a column.
     *
     * @param taskId       the task to move
     * @param targetStatus the column it should be shown in
     * @param targetIndex  the place inside that column, {@code Integer.MAX_VALUE} for the end
     * @return {@code true} when the task was found and moved
     */
    public boolean moveTask(long taskId, TaskStatus targetStatus, int targetIndex) {
        Connection connection = null;
        try {
            connection = database.getConnection();
            connection.setAutoCommit(false);

            Task task = findById(connection, taskId).orElse(null);
            if (task == null) {
                connection.rollback();
                return false;
            }

            TaskStatus sourceStatus = task.getStatus();
            List<Long> targetIds = idsInColumn(connection, task.getBoardId(), targetStatus, taskId);
            int index = Math.max(0, Math.min(targetIndex, targetIds.size()));
            targetIds.add(index, taskId);

            if (sourceStatus != targetStatus) {
                // Closing the gap in the old column keeps the order tidy.
                List<Long> sourceIds = idsInColumn(connection, task.getBoardId(), sourceStatus, taskId);
                writeOrder(connection, sourceStatus, sourceIds);
            }
            writeOrder(connection, targetStatus, targetIds);

            connection.commit();
            task.setStatus(targetStatus);
            task.setPosition(index);
            return true;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw new DataAccessException("Could not move task " + taskId + ".", e);
        } finally {
            closeQuietly(connection);
        }
    }

    public boolean delete(long id) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete task " + id + ".", e);
        }
    }

    /** Number of tasks in a board, used in the status bar. */
    public int countByBoard(long boardId) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("SELECT COUNT(*) FROM tasks WHERE board_id = ?")) {
            statement.setLong(1, boardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the tasks of board " + boardId + ".", e);
        }
    }

    /** The position a new card should get to appear at the end of a column. */
    public int nextPosition(long boardId, TaskStatus status) {
        try (Connection connection = database.getConnection()) {
            return nextPosition(connection, boardId, status);
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the position of a new task.", e);
        }
    }

    // ------------------------------------------------------------------
    // Internals. The methods below either take an open connection or use one.
    // ------------------------------------------------------------------

    private int nextPosition(Connection connection, long boardId, TaskStatus status) throws SQLException {
        String sql = "SELECT COALESCE(MAX(position), -1) + 1 FROM tasks WHERE board_id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, boardId);
            statement.setString(2, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private Optional<Task> findById(Connection connection, long id) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(read(resultSet)) : Optional.empty();
            }
        }
    }

    private List<Long> idsInColumn(Connection connection, long boardId, TaskStatus status, long excludeId)
            throws SQLException {
        String sql = "SELECT id FROM tasks WHERE board_id = ? AND status = ? AND id <> ? ORDER BY position, id";
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, boardId);
            statement.setString(2, status.name());
            statement.setLong(3, excludeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
            }
        }
        return ids;
    }

    /** Numbers the given tasks 0, 1, 2 ... and puts them in the given column. */
    private void writeOrder(Connection connection, TaskStatus status, List<Long> orderedIds) throws SQLException {
        String sql = "UPDATE tasks SET status = ?, position = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < orderedIds.size(); i++) {
                statement.setString(1, status.name());
                statement.setInt(2, i);
                statement.setLong(3, orderedIds.get(i));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Nothing sensible to do here.
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException ignored) {
            // Nothing sensible to do here.
        }
    }

    private Task read(ResultSet resultSet) throws SQLException {
        Task task = new Task(
                resultSet.getLong("id"),
                resultSet.getLong("board_id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                Priority.fromName(resultSet.getString("priority")),
                TaskStatus.fromName(resultSet.getString("status")),
                SqlDates.fromText(resultSet.getString("created_at")),
                SqlDates.fromText(resultSet.getString("due_date")),
                resultSet.getInt("position"));
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDate.now());
        }
        return task;
    }
}
