# Clips stats getter

Console utility for fetch clip information from twitch.tv and store in Postgres.

## Usage

For run utility need configure:

* connection to Postgres (url, username, password),
* twitch authentication (clientId, secret),
* fetch information (broadcasterId, optionally period).

Example of usage:

```shell
clips-stats-getter --spring.r2dbc.url=r2dbc:postgresql://localhost:10227/twitch --spring.r2dbc.username=twitch --spring.r2dbc.password=twitch --twitch.client-id=hof5gwx0su6owfnys0yan9c87zr6t --twitch.secret=41vpdji4e9gif29md0ouet6fktd2 --fetch.broadcaster-id=26819117
```

Options

| option                     | description                              | example                                   |
|----------------------------|------------------------------------------|-------------------------------------------|
| **spring.r2dbc.url***      | url connection to database               | r2dbc:postgresql://localhost:10227/twitch |
| **spring.r2dbc.username*** | database username                        | twitch                                    |
| **spring.r2dbc.password*** | database password                        | twitch                                    |
| **twitch.client-id***      | twitch app client id                     | hof5gwx0su6owfnys0yan9c87zr6t             |
| **twitch.secret***         | twitch app secret                        | 41vpdji4e9gif29md0ouet6fktd2              |
| **fetch.broadcaster-id***  | broadcaster id to fetch                  | 26819117                                  |
| fetch.period               | period for which information is received | 2w10d                                     |

