import { PageAttachedRouter, Router } from "arrowjs-aluminum";
import { html } from "@arrow-js/core";
import { apiRouter, setLinkGen } from "./api.js";
if (window.location.pathname.endsWith("404.html"))
    window.location.href = window.location.href.split("/").slice(0, -1).join("/");
const router = new PageAttachedRouter(document.getElementById("root"), html `404`);
setLinkGen(router.link);
router.addRoute("m3we", new Router()
    .addRoute("", () => html `m3we Documentation [WIP]<br>
            ${router.link.create("./api/") `API`}`)
    .addRoute("api", apiRouter))
    .redirect();
//# sourceMappingURL=index.js.map