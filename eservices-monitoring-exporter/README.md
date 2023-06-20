# EServices Monitoring Exporter

Exports details of Catalog EServices required for probing feature

## Producers Configurations
The configuration allows to define a list of producers whose EServices can be probed.

Notes: 
- Monitoring application requires that all EServices are listed, even if they should not be checked.- 
This means that excluded producers EServices will still be exported, but with `INACTIVE` state
- The probing state of allowed EServices will still be calculated based on Catalog state 


Configuration behaviour based on env var:

| Env var                                      | Behaviour                                                         |
|----------------------------------------------|-------------------------------------------------------------------|
| PRODUCERS_ALLOW_LIST=producerId1,producerId2 | only EServices of producerId1 and producerId2 may be `ACTIVE`     |
| PRODUCERS_ALLOW_LIST=                        | No EService may be `ACTIVE` (probing disabled for every EService) |
| PRODUCERS_ALLOW_LIST not set                 | All EService may be `ACTIVE` (allow list bypassed)                |