## Data Source Configuration

Before using MySQL or MongoDB to store your worlds, you've got to configure them. To do so, navigate to the SWM config folder located inside your plugins directory, and open the 'sources.yml' file. Inside there are all the parameters you need to set. Here's an example of how your sources.yml should look like:

```yaml
file:
    # The path to the directory where slime worlds are stored
    path: slime_worlds
mysql:
    enabled: true
    host: 127.0.0.1
    port: 3306
    username: my_mysql_username
    password: my_mysql_password
    database: slimeworldmanager
mongodb:
    enabled: true
    host: 127.0.0.1
    port: 27017
    username: my_mongo_username
    password: my_mongo_password
    auth: admin
    database: slimeworldmanager
    collection: worlds
```

**Remember to enable MySQL and/or MongoDB if you are going to use them!**
