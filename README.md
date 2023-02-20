# Clips stats getter

Console utility for fetch clip information from twitch.tv and store in Postgres.

## Usage

For run utility need configure:

* connection to Postgres (url, username, password),
* twitch authentication (clientId, secret),
* fetch information (broadcasterId, optionally period).

Example of usage:

```shell
clips-stats-getter \
  --spring.r2dbc.url=r2dbc:postgresql://localhost:10227/twitch \ 
  --spring.r2dbc.username=twitch \
  --spring.r2dbc.password=twitch \
  --twitch.client-id=$CLIENT_ID \
  --twitch.secret=$CLIENT_SECRET \ 
  --fetch.broadcaster-id=26819117

```

Options

| option                     | description                              | example                                   |
|----------------------------|------------------------------------------|-------------------------------------------|
| **spring.r2dbc.url***      | url connection to database               | r2dbc:postgresql://localhost:10227/twitch |
| **spring.r2dbc.username*** | database username                        | twitch                                    |
| **spring.r2dbc.password*** | database password                        | twitch                                    |
| **fetch.broadcaster-id***  | broadcaster id to fetch                  | 26819117                                  |
| fetch.period               | period for which information is received | 2w10d                                     |
| twitch.client-id           | twitch app client id                     | hof5gwx0su6owfnys0yan9c87zr6t             |
| twitch.secret              | twitch app secret                        | 41vpdji4e9gif29md0ouet6fktd2              |
| twitch.base-url            | url to Twitch API or another gateway     | https://api.twitch.tv/helix/              |
| twitch.authorization-url   | url to Twitch OAuth2 token url           | https://id.twitch.tv/oauth2/token         |

## Authorization

In version _0.1.0_ you can not specify authorization parameters if you use _API Gateway_ which itself adds authorization
headers for Twitch API. For this case you must set `twitch.base-url` to you _API Gateway_. As such api gateway you can
use [twitch-auth-gateway](https://github.com/RazorNd/twitch-auth-gateway).

