# AuctionHouse

Modern, lightweight Auction House for Paper 26.2 and Java 25.

## Features

- `/ah` browse GUI
- `/ah sell`
- `/ah my`
- `/ah claims`
- `/ah search <text>`
- `/ah reload`
- 54-slot browse layout with 29 configurable listing slots
- Pagination
- Sorting: newest, oldest, lowest price, highest price, expiring soon
- Search by item material or seller
- Item amount, seller, total price, time remaining and listing ID in lore
- Compact price input/display: `k`, `m`, `b`, `t`
- Configurable minimum/maximum listing price
- Configurable active-listing permissions
- Configurable listing fee
- Configurable sale tax
- Expiration handling
- Claim storage for expired items
- Blocked materials and blocked lore text
- Vault economy integration
- Folia-supported scheduling
- YAML persistence without an external database

## Compatibility

Target:
- Paper 26.2
- Java 25

Java-only. Bedrock support is not included.

## Economy

Vault is a soft dependency. A Vault-compatible economy provider is required for buying and selling with money.

## Build

```bash
mvn -B -U clean verify
```

The GitHub Actions workflow builds with Java 25 and uploads the resulting JAR as an artifact.

## Project

Author: KingBrezz
