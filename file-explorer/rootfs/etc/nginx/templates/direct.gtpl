server {
    {{ if not .ssl }}
    listen {{ .port }} default_server;
    {{ else }}
    listen {{ .port }} default_server ssl;
    http2 on;
    {{ end }}

    include /etc/nginx/includes/server_params.conf;
    include /etc/nginx/includes/proxy_params.conf;

    proxy_redirect off;

    {{ if .ssl }}
    include /etc/nginx/includes/ssl_params.conf;

    ssl_certificate /ssl/{{ .certfile }};
    ssl_certificate_key /ssl/{{ .keyfile }};
    {{ end }}

    # Served from the root here, so none of the Ingress rewriting applies and
    # File Browser's own login screen is what guards it. The header Ingress
    # signs people in with is blanked, so that nothing arriving on this port
    # can claim to be somebody.
    location / {
        proxy_set_header X-Ingress-User "";

        proxy_pass http://backend;
    }
}
