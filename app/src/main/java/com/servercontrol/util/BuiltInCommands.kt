package com.servercontrol.util

import com.servercontrol.domain.model.SavedCommand

object BuiltInCommands {
    val all = listOf(
        SavedCommand(id = "builtin-1", serverId = null, name = "Disk Usage", command = "df -h", description = "Show disk usage in human-readable format", isBuiltIn = true),
        SavedCommand(id = "builtin-2", serverId = null, name = "Top 10 CPU Processes", command = "ps aux --sort=-%cpu | head -11", description = "Top 10 processes by CPU usage", isBuiltIn = true),
        SavedCommand(id = "builtin-3", serverId = null, name = "Top 10 Memory Processes", command = "ps aux --sort=-%mem | head -11", description = "Top 10 processes by memory usage", isBuiltIn = true),
        SavedCommand(id = "builtin-4", serverId = null, name = "Tail Syslog", command = "tail -n 50 /var/log/syslog 2>/dev/null || journalctl -n 50 --no-pager", description = "Last 50 lines of system log", isBuiltIn = true),
        SavedCommand(id = "builtin-5", serverId = null, name = "Check Open Ports", command = "ss -tlnp", description = "List listening TCP ports with process names", isBuiltIn = true),
        SavedCommand(id = "builtin-6", serverId = null, name = "Restart Nginx", command = "sudo systemctl restart nginx", description = "Restart the Nginx web server", isBuiltIn = true),
        SavedCommand(id = "builtin-7", serverId = null, name = "Memory Info", command = "free -h", description = "Show memory and swap usage", isBuiltIn = true),
        SavedCommand(id = "builtin-8", serverId = null, name = "System Info", command = "uname -a && lsb_release -a 2>/dev/null", description = "Kernel and OS information", isBuiltIn = true),
        SavedCommand(id = "builtin-9", serverId = null, name = "Who Is Logged In", command = "who && last -n 5", description = "Currently logged in users and recent logins", isBuiltIn = true),
        SavedCommand(id = "builtin-10", serverId = null, name = "Failed Services", command = "systemctl --failed --no-pager", description = "Show failed systemd services", isBuiltIn = true),
    )
}
