{
    title {Using open source for free tasks}
    author {Mark Janssen}
    presets blog
    tags {gtd}
    date 2019-01-08
}

## Why all the effort?

Online task lists are dissappearing left and right (for example Astrid
and Wunderlist). You want your tasks under your control which means:

<!-- more -->

1.  You have an open file format storing the tasks.
2.  You can synchronize or serve the tasks with open source tools.

I started with `todo.txt` to store my tasks. This works fine, but a
format like `todo.txt` has one big disadvantage: it doesn't have a
concept of task identity so any syncing solution is flaky.

After some searching I found https://taskwarrior.org/[Taskwarrior]
which not only provides a flexible plain text task storage solution, but
it also provide a very robust syncing solution with `taskd`.

## The set-up

I use my own taskd server on a Archlinux based VPS. For now I try to
stay away from user defined attributes (UDAs) as much as possible to
keep the setup simple.

### Accessing the tasks

#### Linux CLI

``` nim

import os
import sequtils
import strutils

# escape arguments for bash
var cmdArgs =  commandLineParams()
               .map(proc (s: string): string = r"$'" & s.replace(r"'",r"\'") & "'")
               .join(" ")
var shellCmd = r"bash -c ""task "  & cmdArgs & "\""
quit(os.execShellCmd(shellCmd))

```

### Web interface

As a web interface I used [inthe.am](http://inthe.am) but even though
the code is open source, that site is not under my control and thus
could disappear at any time.

Instead I now use
[taskwarrior-web](https://github.com/theunraveler/taskwarrior-web). This
is intended for localhost usage and as a result has no authentication.
To make it safe to open this from my own website, I have put it behind a
nginx reverse proxy with basic HTTP authentication.

``` nginx
server {
    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/mpcjanssen.nl/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/mpcjanssen.nl/privkey.pem;
    server_name tasks.mpcjanssen.nl;
    access_log /var/log/nginx/service.tasks.mpcjanssen.nl.access.log;
    error_log /var/log/nginx/service.tasks.mpcjanssen.nl.error.log;
    location / {
        proxy_pass http://127.0.0.1:5678;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        auth_basic "Restricted Content";
        auth_basic_user_file /etc/nginx/.htpasswd;
    }
}
````

I start the ruby gem itself using the following user systemd script at
`~/.config/systemd/user/taskweb@.service`:

``` ini
[Unit]
Description=Taskwarrior web

[Service]
Type=simple
SuccessExitStatus=0 1

ExecStart=/home/mpcjanssen/.gem/ruby/2.4.0/bin/task-web -o 127.0.0.1 -d -F
```

#### Android

Android has two good clients already [Taskwarrior for
Android](https://play.google.com/store/apps/details?id=kvj.taskw&hl=en)
and
[TaskwarriorC2](https://play.google.com/store/apps/details?id=com.taskwc2&hl=en).
However they are missing quite some functionallity I have implemented in
my `todo.txt` app Simpletask. So I am planning to support Taskwarrior in
Simpletask.

### Capturing tasks

For capturing tasks, I either use the CLI tools, or I use the
org-protocl browser plugins with a custom org-protocl handler script in
Tcl:

``` tcl
puts "Handling org-protocol call"

proc invalidcall {} {
  puts stderr "Invalid org-protocol call $::argv"
  exit 1
}

proc expandPercent {data} {
    set pos 0
    while { -1 != [set pos [string first "%" $data $pos]]} {
        set hexNumber "0x[string range $data $pos+1 $pos+2]"
        if { 4 != [string length $hexNumber] || ! [string is integer $hexNumber] } {
            # No two hex character - eventual error treatment here
            # at the moment just leave the percent character
        } else {
            set data [string range $data 0 $pos-1][format %c $hexNumber][string range $data $pos+3 end]
        }
        incr pos
    }
    return $data
}

proc handlecapture {type url title {text {}}} {
  puts "URL: $url"
  set url [expandPercent $url]
  set title [string trim [expandPercent $title]]
  set text [string trim [expandPercent $text]]

  puts "URL: $url"
  puts "title: $title"
  puts "text: $text"
  # console show
  # vwait forever
  captureAddTaskwarriorWindows $url $title $text
  exit 0

}

proc captureAddTaskwarriorWindows {url title text} {
    if {[catch {exec task add $title} result]} {
    puts $result
    gets stdin
    } else {
      set taskId [string range [lindex [split $result] end] 0 end-1]
      exec task $taskId annotate Captured url: $url
    }
}

proc captureAppendTodoTxt {url title text} {
  set f [open "~/Dropbox/todo/todo.txt" a]
  set timestamp [clock format [clock seconds] -format %Y-%m-%d]
  puts $f "$timestamp $title $url +orgcapture"
  close $f
}

if {$argc != 1} {
  invalidcall
}

lassign $argv protocall

if {!([string first org-protocol:// $protocall] == 0)} {
  invalidcall
}

set prefixlength [string length org-protocol://]

set protocall [string range $protocall $prefixlength end]

puts $protocall

set arguments [lassign [split $protocall /] action]

switch -exact -- $action {
  capture: { handlecapture {*}$arguments } 
  default { 
    puts stderr "Unsupported action $action from $argv"
    exit 1
  }
}

```

To register the handler use:

``` reg
REGEDIT4

[HKEY_CLASSES_ROOT\org-protocol]
@="URL:Org Protocol"
"URL Protocol"=""
[HKEY_CLASSES_ROOT\org-protocol\shell]
[HKEY_CLASSES_ROOT\org-protocol\shell\open]
[HKEY_CLASSES_ROOT\org-protocol\shell\open\command]
@="\"C:\\Bin\\org-protocol-handler.exe\" \"%1\""
```

