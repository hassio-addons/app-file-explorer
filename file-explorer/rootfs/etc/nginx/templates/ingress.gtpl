server {
    listen {{ .interface }}:{{ .port }} default_server;

    include /etc/nginx/includes/server_params.conf;
    include /etc/nginx/includes/proxy_params.conf;

    # File Browser builds its URLs from the site root, which is the wrong place
    # when Home Assistant hands it out from an Ingress path instead. That path
    # is only known per request, so it is written into the page on the way
    # past.
    #
    # This is two values in the page File Browser serves, not a rewrite of the
    # application: every asset the page loads sits below "/public/static", and
    # "baseURL" in the small block of JSON it bootstraps from is what the
    # router, the API calls and the event stream are all resolved against. The
    # megabytes of JavaScript behind them are never touched.
    sub_filter_once off;
    sub_filter '"/public/static/' '"$http_x_ingress_path/public/static/';
    sub_filter '"baseURL":"/"' '"baseURL":"$http_x_ingress_path/"';

    # A small script goes into the page as well, for the two things an Ingress
    # panel needs that a plain browser tab does not; it explains itself. It is
    # served from below, as a file of its own, because File Browser's content
    # security policy admits scripts from its own origin and from nowhere
    # else, and an inline one would need the nonce it mints per page.
    sub_filter '</head>' '<script src="$http_x_ingress_path/ingress.js"></script></head>';

    location = /ingress.js {
        allow   172.30.32.2;
        deny    all;

        alias /usr/share/filebrowser/ingress.js;
        default_type application/javascript;
    }

    location / {
        allow   172.30.32.2;
        deny    all;

        {{ if .auto_login }}
        # Home Assistant has already established who is asking by the time a
        # request reaches Ingress, so File Browser is told to accept that, and
        # each person gets an account of their own. The name is worked out from
        # the Home Assistant user by the maps in nginx.conf.
        #
        # The header is private to this app. Setting it here replaces any copy
        # that arrived with the request, so nothing a browser sends can land
        # in it, and the direct server blanks it the same way.
        proxy_set_header X-Ingress-User $file_explorer_ingress_user;
        {{ else }}
        proxy_set_header X-Ingress-User "";
        {{ end }}

        proxy_pass http://backend;
    }
}
