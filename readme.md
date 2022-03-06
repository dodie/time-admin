[![License](https://img.shields.io/github/license/dodie/time-admin.svg)](https://github.com/dodie/time-admin/blob/master/LICENSE)

*Timeadmin* is a simple web-based tool for time tracking to measure the time spent
on various tasks by collaborators working on small or large projects.
From the collected data the tool can generate reports, such as
timeline of activities in a given day, or monthly summaries.

![Tasks](https://github.com/dodie/time-admin/blob/master/docs/screenshots/tasks.png "Tasks")


It can generate monthly **timesheets** from the collected data, that can be personalized with
an Excel template.

![Timesheet](https://github.com/dodie/time-admin/blob/master/docs/screenshots/timesheet.png "Timesheet")


The **collected data** can provide insights about the time spent by the team.

![Tasksheet](https://github.com/dodie/time-admin/blob/master/docs/screenshots/tasksheet.png "Tasksheet")


Usage
-----

Deploy the WAR file to a web container, such as Tomcat.

You can supply custom configuration to your instance by setting the ```-DexternalConfig=<path>```
variable for the application. The default configuration file can be found in the
[default.props](https://github.com/dodie/time-admin/blob/master/src/main/resources/props/default.props)
file.


### Configure the DB

By default Timeadmin provides an in-memory HSQL database, but it can be configured to use
a PostgreSQL instance by providing the relevant settings in the properties file:

```
db.driver = org.postgresql.Driver
db.url = jdbc:postgresql://127.0.0.1:5432/timeadmin
db.user = postgres
db.password = 1234
```


### Configure SMTP

For the password recovery feature an SMTP has to be configured:

```
mail.smtp.host = localhost
mail.smtp.port = 22
mail.smtp.auth = false
mail.smtp.user = user@domain
mail.smtp.pass = 1234
```


### Feature configurations

- `export.excel.timesheet_template`: specify a custom excel template for the Timesheet export.
- `user.subtract_breaks`: specify the default value of subtract breaks for new users, false by default.
- `mantis.bugs.view.url`: if this URL is specified, when an issue ID is detected, it will be converted to
  a link to the bug tracker.


Timeadmin API
-------------
If you are about to integrate your application with Timeadmin, check the
[API Reference](https://github.com/dodie/time-admin/tree/master/api-reference.md) for
detailed examples about the API.


Development guide
-----------------
See [this](https://github.com/dodie/time-admin/blob/master/development-guide.md#running) guide for instructions.


Contributing
------------
Contributions are welcome! Please make sure to visit our
[contribution](https://github.com/dodie/time-admin/tree/master/CONTRIBUTING.md)
and
[development guide](https://github.com/dodie/time-admin/tree/master/development-guide.md)
for details about the application.

If you are looking for issues, see [Issues marked with the help-wanted tag](https://github.com/dodie/time-admin/issues?q=is%3Aissue+label%3A%22help+wanted%22+is%3Aopen) or the
[Ideas](https://github.com/dodie/time-admin/wiki/Ideas) wiki page.

