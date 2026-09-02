# AuctionHouse

Standalone Auction House extracted/reimplemented from the Auction House behavior/configuration present in the supplied UltimateDonutSmp 1.5 JAR.

- Author: KingBrezz
- Server: Paper 26.2
- Java: 25
- Commands: `/ah`, `/ah reload`
- Selling is done from the `/ah` GUI: click **Sell Held Item**, then type the price in chat.
- The entire stack in the main hand is listed.
- Configurable maximum price; default is **100,000,000 (100m)**.
- Configurable active-listing limits with permission nodes.
- Vault is optional at compile time and detected at runtime. Install Vault + an economy provider for buying/selling money.

## Permissions

- `auctionhouse.use` - open AH
- `auctionhouse.sell` - list items
- `auctionhouse.buy` - buy listings
- `auctionhouse.admin` - `/ah reload`
- `auctionhouse.limit.10`
- `auctionhouse.limit.25`
- `auctionhouse.limit.50`
- `auctionhouse.limit.100`
- `auctionhouse.limit.250`

The highest matching limit permission wins. Default limit is 5.

## Max price

Edit `config.yml`:

```yaml
PRICING:
  MIN_PRICE: 100
  MAX_PRICE: 100000000
```

A listing above 100m is rejected.

## Build

```bash
mvn clean package
```

Output: `target/AuctionHouse-1.0.0.jar`
