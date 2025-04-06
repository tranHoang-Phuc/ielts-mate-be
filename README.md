# SEP Project Setup

Welcome to the SEP Project repository! This guide will walk you through setting up the project on your local machine.

## Prerequisites

Before proceeding with the setup, ensure the following requirements are met:

- **Operating System**:
  - **Windows**: Required for running the `add-host.bat` script.
  - **Linux/Unix**: Required for running the `start-sep.sh` script.
- **Administrator/Root Privileges**:
  - On Windows: Needed to modify the `hosts` file via `add-host.bat`.
  - On Linux/Unix: Needed to edit `/etc/hosts`.
- **Bash Shell**:
  - Required for running `start-sep.sh`.
  - On Windows, you can use Git Bash, Windows Subsystem for Linux (WSL), or a similar tool.
- **PostgreSQL**:
  - Ensure PostgreSQL (and any other required services) is installed locally or configured correctly.
- **Docker** (optional):
  - Required if you plan to run services using Docker Compose.

## Setup Instructions

### Step 1: Configure Host Entries

You need to add specific entries to your system's `hosts` file to resolve local domains.

#### On Windows
1. **Run `add-host.bat` as Administrator**:
   - Navigate to the project directory containing `add-host.bat`.
   - Right-click the file and select **"Run as Administrator"**.
   - The script will append the following entries to your `C:\Windows\System32\drivers\etc\hosts` file:
 ```bash
      127.0.0.1 identity.sep.local
      127.0.0.1 registry.sep.local
 ```

#### On Linux/Unix:

If you’re on a Linux or Unix-based system, you need to manually add these entries to `/etc/hosts`:

```bash
echo "127.0.0.1 identity.sep.local" | sudo tee -a /etc/hosts
echo "127.0.0.1 registry.sep.local" | sudo tee -a /etc/hosts
```

### Step 2: Start SEP Project

#### Linux/Unix:

-**Run `start-sep.sh` script**:
    - Open a terminal.
    - Navigate to the directory containing the `start-sep.sh` script.
    - Run script.
```bash
./start-sep.sh
```

#### On Windows:

- For Windows, the `start-sep.sh` script will not work directly since it's a Bash script. Instead, you can either:
    - Use a Bash shell like Git Bash or Windows Subsystem for Linux (WSL), then follow the instructions above.
    - Alternatively, if the services are dockerized, you can use Docker Compose (ensure Docker is installed on your machine) to start the services.


