# CauchoChain

A blockchain implementation in Java featuring a complete cryptocurrency system with wallets, mining, transactions, and a graphical user interface.  CauchoChain demonstrates core blockchain concepts including proof-of-work consensus, transaction validation, cryptographic signatures, and distributed ledger technology.

## Features

- **Blockchain Core**: Complete blockchain implementation with proof-of-work consensus mechanism
- **Transaction System**: Support for signed transactions with cryptographic validation
- **Wallet Management**: Secure wallet implementation with public/private key pairs
- **Mining**: Configurable difficulty mining system with block rewards
- **Transaction Pool**: Pending transaction management before block confirmation
- **Smart Contracts**: Registry system for smart contract deployment (experimental)
- **Network Simulation**: Basic peer-to-peer network message handling
- **GUI**: Swing-based graphical interface for blockchain visualization and interaction
- **Logging**: Comprehensive logging system for debugging and monitoring
- **Redis Integration**: Optional Redis support for distributed operations

## Architecture

```
CauchoChain/
├── src/
│   ├── model/          # Core blockchain models (Block, Blockchain, Transaction)
│   ├── wallet/         # Wallet implementation and interfaces
│   ├── miner/          # Mining logic and miner interfaces
│   ├── network/        # Network message handling
│   ├── GUI/            # Swing-based user interface
│   └── utils/          # Utility classes (Logger, CryptoUtils)
├── lib/                # External libraries
└── target/             # Compiled classes and artifacts
```

## Requirements

- Java Development Kit (JDK) 11 or higher
- Maven 3.6 or higher
- Redis (optional, for distributed features)
- Docker (optional, for containerized Redis)

## Setup

### 1. Clone the Repository

```bash
git clone https://github.com/Matias-Sanmiguel/CauchoChain.git
cd CauchoChain
```

### 2. Build the Project

Using Maven: 

```bash
mvn clean install
```

This will compile the source code and package the application into a JAR file in the `target` directory.

### 3. Set Permissions for Shell Scripts

Make the shell scripts executable: 

```bash
chmod +x start-all.sh
chmod +x run-gui.sh
chmod +x monitor-logs.sh
chmod +x monitor_redis_logs.sh
```

### 4. Optional: Setup Redis

If you want to use Redis features, you can start Redis using Docker:

```bash
docker build -f Dockerfile. redis -t cauchocoin-redis .
docker run -d -p 6379:6379 --name cauchocoin-redis cauchocoin-redis
```

Alternatively, install and run Redis locally following the [official Redis documentation](https://redis.io/docs/getting-started/).

## Usage

### Running the GUI Application

To launch the graphical user interface:

```bash
./run-gui.sh
```

Or manually:

```bash
java -cp target/CauchoChain-1.0-SNAPSHOT.jar GUI.BlockchainGUI
```

### Running the Demo Test

To execute the demonstration test that shows basic blockchain operations:

```bash
java -cp target/CauchoChain-1.0-SNAPSHOT.jar DemoTest
```

### Starting All Services

To start the complete system (if configured for multi-node setup):

```bash
./start-all.sh
```

### Monitoring Logs

Monitor application logs:

```bash
./monitor-logs.sh
```

Monitor Redis logs (if using Redis):

```bash
./monitor_redis_logs.sh
```

## Core Components

### Blockchain

The `Blockchain` class manages the chain of blocks, pending transactions, and mining difficulty. It provides methods for: 
- Adding transactions to the transaction pool
- Mining pending transactions into blocks
- Validating chain integrity
- Querying balances

### Wallet

The `Wallet` class represents a user's wallet with:
- Public/private key pair generation
- Transaction creation and signing
- Balance tracking
- User association

### Miner

The `Miner` class handles block mining with:
- Configurable hash rate
- Proof-of-work algorithm
- Block validation
- Mining rewards

### Transaction

The `Transaction` class represents value transfers with:
- Digital signature verification
- Transaction fee support
- Hash calculation for integrity

## Configuration

### Mining Difficulty

Adjust the mining difficulty (number of leading zeros required in block hash) in the `Blockchain` class or through the GUI configuration panel.

Default difficulty:  `3`

### Mining Reward

The mining reward can be configured in the `Blockchain` class.

Default reward: `50.0` tokens

## Development

### Project Structure

- **model**:  Contains the core data structures (Block, Blockchain, Transaction, User)
- **wallet**: Wallet implementation and cryptographic operations
- **miner**:  Mining logic and block validation
- **network**: Network message protocols for distributed operation
- **GUI**: Swing-based user interface components
- **utils**: Helper classes including logging and cryptographic utilities

### Building from Source

```bash
mvn clean compile
```

### Running Tests

```bash
mvn test
```

### Creating Distribution Package

```bash
mvn package
```

## Examples

### Creating a Transaction

```java
Blockchain blockchain = new Blockchain();
Wallet sender = new Wallet("Alice");
Wallet receiver = new Wallet("Bob");

Transaction tx = sender.createTransaction(receiver. getAddress(), 10.0f, blockchain);
blockchain.createTransaction(tx);
```

### Mining a Block

```java
Miner miner = new Miner(1. 0f, "Miner1");
miner.mine(blockchain);
```

### Checking Balance

```java
float balance = blockchain.getBalance(wallet.getAddress());
System.out.println("Balance: " + balance);
```

## Contributing

Contributions are welcome.  Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## License

This project is available for educational and research purposes.

## Acknowledgments

Built as an educational project to demonstrate blockchain technology concepts including distributed consensus, cryptographic security, and decentralized systems.
