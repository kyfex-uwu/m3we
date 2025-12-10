import {PageAttachedRouter} from "arrowjs-aluminum";
import {html} from "@arrow-js/core";
import {apiRouter} from "./api.js";

new PageAttachedRouter(document.getElementById("root")!, html`404`)
    .addRoute("api", apiRouter)
    .redirect();
