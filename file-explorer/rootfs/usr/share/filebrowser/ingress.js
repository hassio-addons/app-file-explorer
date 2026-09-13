// Home Assistant Community App: File Explorer
//
// Served by NGINX into the page over Ingress only. Two things an Ingress panel
// needs that a plain browser tab does not, both of which File Browser cannot
// know about. The findings are alexbelgium's, from the FileBrowser Quantum
// add-on in https://github.com/alexbelgium/hassio-addons.
//
// 1. A folder opened in a new tab. Search results ("go to item") and the tool
//    views ("open parent folder") open folders with window.open(url, "_blank").
//    That tab lands on the raw Ingress address with no Home Assistant frontend
//    around it to keep the Ingress session alive, and answers 401 as soon as
//    the session lapses. Those are turned into a navigation of the panel
//    itself. Only File Browser's own folder and share routes are touched, so a
//    download or a raw preview keeps its tab, as does a link out of the app.
//
// 2. Downloads in the Home Assistant companion app. File Browser downloads by
//    clicking a link that carries no "download" attribute, which is the one
//    thing the app's web view needs to hand a file to its download manager;
//    without it the file is simply rendered, with no way to save it. Links
//    that open a new tab are worse there: the app sends every new tab to an
//    external browser, which carries no Ingress session and gets a 401. The
//    attribute is added to File Browser's download links everywhere, which
//    changes nothing in a desktop browser, and "_blank" is dropped from links
//    into the app only when running inside the companion app, which announces
//    itself in the user agent.
(function () {
  "use strict";

  function baseURL() {
    var base = (window.globalVars || {}).baseURL || "/";
    return base.slice(-1) === "/" ? base : base + "/";
  }

  function inThisApp(url) {
    return (
      url.origin === window.location.origin &&
      url.pathname.indexOf(baseURL()) === 0
    );
  }

  var open = window.open;
  window.open = function (target, name) {
    try {
      if (target && name === "_blank") {
        var url = new URL(target, window.location.href);
        var base = baseURL();
        if (
          inThisApp(url) &&
          (url.pathname.indexOf(base + "files/") === 0 ||
            url.pathname.indexOf(base + "public/share/") === 0)
        ) {
          window.location.assign(url.href);
          return window;
        }
      }
    } catch (e) {
      // Not a URL we understand; let the browser have it.
    }
    return open.apply(window, arguments);
  };

  var companionApp = navigator.userAgent.indexOf("Mobile/HomeAssistant") !== -1;

  // Capturing, so it runs before the link is followed, and a programmatic
  // click() on an anchor in the document passes through here as well.
  document.addEventListener(
    "click",
    function (event) {
      try {
        if (
          event.button ||
          event.metaKey ||
          event.ctrlKey ||
          event.shiftKey ||
          event.altKey
        ) {
          return;
        }
        var anchor =
          event.target && event.target.closest
            ? event.target.closest("a")
            : null;
        if (!anchor || !anchor.href) {
          return;
        }
        var url = new URL(anchor.href, window.location.href);
        if (!inThisApp(url)) {
          return;
        }
        var isDownload =
          /\/api\/resources\/download$/.test(url.pathname) &&
          url.searchParams.get("inline") !== "true";
        if (isDownload && !anchor.hasAttribute("download")) {
          anchor.setAttribute("download", "");
          anchor.removeAttribute("target");
          return;
        }
        if (companionApp && anchor.getAttribute("target") === "_blank") {
          anchor.removeAttribute("target");
        }
      } catch (e) {
        // Leave the click alone.
      }
    },
    true,
  );
})();
