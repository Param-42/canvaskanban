package com.canvaskanban.dao;

import com.canvaskanban.database.Database;
import com.canvaskanban.model.Board;
import com.canvaskanban.model.Theme;

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
 * Reads and writes boards. All SQL lives here; the rest of the application only
 * deals with {@link Board} objects.
 */
public class BoardDao {

    private static final String SELECT_COLUMNS = "SELECT id, name, description, theme, created_at FROM boards";

    private final Database database;

    public BoardDao(Database database) {
        this.database = database;
    }

    /** Saves a new board and copies the generated database id into it. */
    public Board insert(Board board) {
        String sql = "INSERT INTO boards (name, description, theme, created_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, board.getName());
            statement.setString(2, board.getDescription());
            statement.setString(3, board.getTheme().getId());
            String createdAt = board.getCreatedAt() == null || board.getCreatedAt().isBlank()
                    ? LocalDate.now().toString()
                    : board.getCreatedAt();
            statement.setString(4, createdAt);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    board.setId(keys.getLong(1));
                }
            }
            board.setCreatedAt(createdAt);
            return board;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the board \"" + board.getName() + "\".", e);
        }
    }

    /** All boards, oldest first. */
    public List<Board> findAll() {
        String sql = SELECT_COLUMNS + " ORDER BY id";
        List<Board> boards = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                boards.add(read(resultSet));
            }
            return boards;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the boards.", e);
        }
    }

    public Optional<Board> findById(long id) {
        String sql = SELECT_COLUMNS + " WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(read(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load board " + id + ".", e);
        }
    }

    /** Updates name, description and theme of an existing board. */
    public boolean update(Board board) {
        String sql = "UPDATE boards SET name = ?, description = ?, theme = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, board.getName());
            statement.setString(2, board.getDescription());
            statement.setString(3, board.getTheme().getId());
            statement.setLong(4, board.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the board \"" + board.getName() + "\".", e);
        }
    }

    /** Deletes a board. Its tasks are removed as well (ON DELETE CASCADE). */
    public boolean delete(long id) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM boards WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete board " + id + ".", e);
        }
    }

    public int count() {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM boards");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the boards.", e);
        }
    }

    private Board read(ResultSet resultSet) throws SQLException {
        return new Board(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                Theme.fromId(resultSet.getString("theme")),
                resultSet.getString("created_at"));
    }
}
