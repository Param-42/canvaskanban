# Project report (college assignment)

This folder builds the PDF that has to be handed in: the complete project details plus
the individual contribution of every team member.

| File | What it is |
| --- | --- |
| `build_report.py` | the source of the whole document - all of the text lives in it |
| `CanvasKanban-Project-Report.pdf` | the document to hand in (generated) |
| `CanvasKanban-Project-Report.docx` | the same document for Word / Google Docs, where every placeholder is highlighted in yellow (generated) |

The two generated files are in `.gitignore`, because they contain names and enrollment
numbers. They stay on the machine of whoever builds them.

## The two hand-out documents

`build_viva_pack.py` builds a shorter pair of documents from the same facts, meant for
speaking in front of the examiner:

| File | What it is |
| --- | --- |
| `build_viva_pack.py` | the source of both documents below |
| `CanvasKanban-Simplified-Report.pdf` / `.docx` | the short version of the report: what the project is, how the four layers work, the features, the team, the statistics, the demo order and the screenshots |
| `CanvasKanban-Viva-Speaking-Notes.pdf` / `.docx` | one section per member: the files to open, the sentences to say, the real code excerpts to point at, the likely questions with answers, the common questions, and a checklist |

Build them the same way:

```bash
.venv/bin/python build_viva_pack.py
```

It writes the four files into this folder and, if `~/Downloads` exists, copies them there
under the names `CanvasKanban_Simplified_Report.*` and
`CanvasKanban_Code_Explanation_All_Members.*`. It also prints how many pages each document
has, how many placeholders are left and a warning if a character in the text cannot be
drawn by the embedded font.

The details that have to be filled in are defined once, in the `DETAILS` and `TEAM` tables
at the top of the script, so the same edit fills both documents. They use exactly the same
placeholders as the main report.

## What still has to be filled in

Everything that is written between square brackets is a placeholder. In the `.pdf` they
are printed in dark red, in the `.docx` they are highlighted in yellow:

- the course, college, department, faculty member, academic year and submission date
- the five full names and enrollment numbers (the first member is the team leader)

There are no other placeholders: the project description, the objectives, the technology
table, the features, the per-member tasks, modules and tools, the shared contributions,
the contribution matrix and all of the numbers are already filled in for CanvasKanban.
The numbers in section 6 come from the repository itself (file, line and test counts) and
from the releases, so they can be verified.

## How to build it

Needs Python 3 with two libraries:

```bash
cd docs/assignment
python3 -m venv .venv
.venv/bin/pip install python-docx reportlab
.venv/bin/python build_report.py
```

That writes both files into this folder and prints the list of placeholders that are
still in the document.

## How to change the text

Either edit the text directly in Word or Google Docs (the `.docx` is there for that), or
change the content at the top of `build_report.py` and run it again - the second way is
worth it when a whole paragraph or a contribution has to be rewritten, because the
layout, the numbering and the placeholders stay consistent.

The screenshots come from `docs/screenshots`, so replacing one of those images also
updates the report.

## A note about the contributions

The work is split along the modules that really exist in the repository: database and
persistence, data access layer and business rules, the JavaFX user interface, drag and
drop plus themes and documentation, and the build, packaging, continuous integration and
delivery. Please check that this matches what actually happened, and tell whoever
maintains `build_report.py` about any change - it is the same file that produces the
PDF.
