# floodingmap

### Set up environment variables
To run the project correctly you must set three environment variables:
- "DBNAME" : the name of the postgres database that you are using
- "DBUSER" : the username of the authentication to the database
- "DBPASSWORD" : the password of the authentication to the database

**If your database is not set up, you can utilize the DatabaseTool from tools project to seed a postgres database**

## Data
### Setup data models with jOOQ
Instead of having the data where the logic is, the logic is moved to where the data is.
Models from the Schema's tables are now created with a command

- In the terminal, write: **mvn clean compile**

Doesn't work? This might help:
- If you haven't installed maven: **brew install maven**

- Check pom.xml: 
  - Make sure the package name and target directory is correct:

`<target>
    <packageName>dk.itu.data</packageName>
    <directory>target/generated-sources/jooq</directory>
</target>`

- - Make sure the Schema name is correct (typically named public):

`<inputSchema>public</inputSchema>`


- Detailed overview of the clean compile process: **mvn clean compile -X**