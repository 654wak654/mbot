# mbot
MBot - MCo's discord bot

## Installation Instructions for Ubuntu

_(All commands below were run as the root user)_

Install dependencies: `apt install git openjdk-11-jdk gradle -y`

Get mbot: `git clone https://github.com/654wak654/mbot.git`

CD into it: `cd mbot`

Give execute permission for update & start scripts: `chmod +x update.sh mbot.sh`

Create mbot service file: `nano /etc/systemd/system/mbot.service`
```service
[Unit]
Description=MBot
After=network.target

[Service]
WorkingDirectory=/root/mbot
ExecStart=/root/mbot/mbot.sh -s

[Install]
WantedBy=multi-user.target
```

Refresh services list: `systemctl daemon-reload`

Enable mbot service: `systemctl enable mbot`

Copy example config: `cp mbot.example.properties mbot.properties`

Don't forget to enter bot key: `nano mbot.properties`

Finally, start mbot: `./update.sh`
