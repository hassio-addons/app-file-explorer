---
server:
  # Only ever listen on loopback; NGINX is what faces Ingress and the network.
  listen: "127.0.0.1"
  port: 48080

  # Served from the root. The path Ingress hands out is only known per request,
  # so NGINX writes it into the page on the way past rather than settling it
  # here.
  baseURL: "/"

  # Accounts, settings and shares live in the database. The cache holds the
  # thumbnails and the search index, both of which are rebuilt from the files
  # themselves, which is why it is left out of backups.
  database: "/data/filebrowser/database.db"
  cacheDir: "/data/filebrowser/cache"

  # Updates to this app arrive through the Home Assistant app store, so the
  # banner offering the ones published upstream is both wrong here and an
  # outbound request nobody asked for.
  disableUpdateCheck: true

  logging:
    - levels: "{{ .log_levels }}"
      apiLevels: "{{ .api_log_levels }}"
      output: "stdout"

  # The Home Assistant directories, each as a source of its own, so that the
  # sidebar reads the way the rest of Home Assistant names them. Every account
  # gets all of them; what somebody may do inside them is a per-account
  # permission, settled below and adjustable from the settings pages.
  sources:
    - path: "/config"
      name: "config"
      config:
        defaultEnabled: true
        # Home Assistant unpacks Python packages into "deps", and Python leaves
        # its bytecode in "__pycache__". Both are thousands of files nobody is
        # looking for, which would fill the search results with noise and the
        # index with work. They stay browsable, and are only left out of the
        # index; the same two that the File editor app hides by default.
        rules:
          - folderName: "__pycache__"
            viewable: true
          - folderName: "deps"
            viewable: true
    - path: "/local_apps"
      name: "local_apps"
      config:
        defaultEnabled: true
    - path: "/app_configs"
      name: "app_configs"
      config:
        defaultEnabled: true
    - path: "/backup"
      name: "backup"
      config:
        defaultEnabled: true
    - path: "/media"
      name: "media"
      config:
        defaultEnabled: true
    - path: "/share"
      name: "share"
      config:
        defaultEnabled: true
    - path: "/ssl"
      name: "ssl"
      config:
        defaultEnabled: true

auth:
  # The account behind the password login, and the one name Ingress is never
  # allowed to sign in as; see the maps in the NGINX configuration. Its
  # password comes in through the environment, from the "password" option.
  adminUsername: "admin"
  methods:
    password:
      enabled: true
    # Home Assistant has established who is asking long before a request
    # reaches this app. Over Ingress, NGINX passes that on in a header of its
    # own, which File Browser takes as the account to sign in, creating it the
    # first time it sees the name. The header is blanked on the published
    # port, so nothing arriving there can claim to be somebody.
    proxy:
      enabled: {{ .auto_login }}
      header: "X-Ingress-User"

frontend:
  # Installing the app to a home screen from within an Ingress panel pins an
  # address that belongs to a Home Assistant session, which expires. Reach the
  # published port to install it on a phone instead.
  disablePWAInstall: true

userDefaults:
  account:
    # Everybody who can open this app has been let in by Home Assistant to
    # manage the files of the installation, so an account made for them gets
    # the run of the place. These are the defaults for a new account; an
    # administrator can trim them per account afterwards.
    permissions:
      api: true
      admin: true
      modify: true
      share: true
      realtime: true
      delete: true
      create: true
      download: true
