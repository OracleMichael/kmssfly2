# Sending a Payload to a Postgres Server

This simple example demonstrates how to connect to a PostGreSQL server on your machine. It is recommended that you use a virtual environment or compute instance as your machine.

# Prerequisites

For additional information, see `README.md` in the parent folder Deliverables.

- PostGres is set up properly on your machine
	- [JDBC interface](https://jdbc.postgresql.org/documentation/80/index.html)
- Bash terminal is available to use

It is assumed the file structure looks something like this:
<pre>
> Deliverables
	> Postgres_Storage
		PostgresStore.java
		README.md [this file]
	> [other projects]
	[other files]
	> lib
		javax.activation-1.2.0.jar
		oci-java-sdk-full-1.12.0.jar
		postgresql-42.2.9.jar
		slf4j-jdk14-1.7.30.jar
		[1 folder]
</pre>

# How to run

- Open your Bash terminal.
- Navigate to the top directory (in this case, Deliverables). If the top directory is nested under "~/Desktop", then you would run:
```
cd ~/Desktop/Deliverables
```
- Run these commands:
the first compiles the Java file, and the second runs the program. You will see some output that will verify that the program is working.
```
javac -cp lib/postgresql-42.2.9.jar Postgres_Storage/PostgresStore.java
java -cp Postgres_Storage:lib/postgresql-42.2.9.jar PostgresStore
```
