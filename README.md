# SEP Project Setup

Welcome to the SEP Project repository! This guide will help you set up the project on your local machine.

## Prerequisites

Before running the setup scripts, ensure you have the following installed:

- **Windows** (for `add-host.bat` script)
- **Linux/Unix** (for `start-sep.sh` script)
- **Administrator privileges** (for running the `add-host.bat` script on Windows)
- **Bash shell** (Linux/Unix-based systems)
- **PostgreSQL** and other necessary services should be available locally or properly configured

### Windows:
- **Administrator privileges** to modify the `hosts` file
- **Bash shell** (for running `start-sep.sh`, can use Git Bash or WSL if on Windows)

## Setup Instructions

### Step 1: Add Host Entries

#### On Windows:

**Run `add-host.bat` as Administrator**:
    - Locate the `add-host.bat` file in your project directory.
    - Right-click on `add-host.bat` and select **"Run as Administrator"**.
    - This script will add the following entries to your system's `hosts` file:
      ```
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

**Run `start-sep.sh` script**:
    - Open a terminal
    - Navigate to the directory containing the `start-sep.sh` script
    - Run script
      ```bash
      ./start-sep.sh
      ```
#### On Windows:

For Windows, the `start-sep.sh` script will not work directly since it's a Bash script. Instead, you can either:
    - Use a Bash shell like Git Bash or Windows Subsystem for Linux (WSL), then follow the instructions above.
    - Alternatively, if the services are dockerized, you can use Docker Compose (ensure Docker is installed on your machine) to start the services.


