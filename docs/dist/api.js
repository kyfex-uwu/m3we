import { Router } from "arrowjs-aluminum";
import { html } from "@arrow-js/core";
const apiPages = {};
export const apiRouter = new Router()
    .addRoute(":api", (vars) => html `${(() => {
    const api = apiPages[vars.api];
    if (api === undefined)
        return html `API ${vars.api} not found`;
    return html `<div class="api-block">
            <h1>${api.title} API</h1>
            <p>${api.description}</p>
            ${api.blocks.map(block => typeof block !== "object" ?
        html `<p>${block}</p>` :
        html `<p id="${api.title}_${block.name}"><span class="code">${api.title}.${block.name}(${Object.entries(block.props).map(([key, val]) => `${key}:${val}`).join(", ")}):${block.returns}</span>
                    <br>${block.description !== undefined ? html `<div>${block.description}</div>` : ""}${block.examples.map(example => html `<div class="example">Example: <span class="code">${api.title}.${block.name}(${Object.values(example.props).join(", ")})</span><br>${example.description}</div>`)}</p>`)}</div>`;
})()}`);
function addApi(page) {
    apiPages[page.title] = page;
}
addApi({
    title: "Property",
    description: "The Property API is a simple way to manage blockstates. It only has 2 functions: " +
        "<span class=\"code\">Properties.get</span> and <span class=\"code\">Properties.set</span>, " +
        "and they each do what you would expect.",
    blocks: [
        {
            name: "get",
            props: { state: "BlockState", propertyName: "string" },
            returns: "&lt;property value>",
            examples: [{
                    props: { state: "state", propertyName: "\"facing\"" },
                    description: "Returns the state's value of the property specified or nil, if no property is found.",
                }],
        },
        {
            name: "set",
            props: { world: "World", pos: "BlockPos", propertyName: "string", value: "&lt;property value>" },
            returns: "boolean",
            examples: [{
                    props: { world: "world", pos: "blockPos", propertyName: "\"facing\"", value: "\"north\"" },
                    description: "Sets the property specified on a block to the value specified, and returns if the property " +
                        "was set successfully.",
                }],
        },
    ]
});
addApi({
    title: "Datastore",
    description: "The Datastore API is super simple: it's an empty table that can be read from or written to by any " +
        "script. It is global and per-world, and it can only contain strings, numbers, booleans, " +
        "and other tables, and the keys can only be strings or numbers. (Also any references to the table will be lost " +
        "when putting a table into the Datastore—just be careful of that)<br>If you would like to edit the Datastore " +
        "table outside of Minecraft, look inside your save (.minecraft/saves/&lt;your save>/m3we_datastore.dat). It'll " +
        "be an nbt file :3",
    blocks: [
        "The Datastore API is (almost) a regular Lua table, so you can treat it as such when adding/fetching data from " +
            "it.<br>Out of courtesy to other m3we creators, consider creating a table inside the Datastore table to manage " +
            "all your resources in, to avoid conflicts. A good name for this table is your Minecraft username :3<br>" +
            "<div class=\"example\">Example:<br>" +
            "<div class=\"code\">if Datastore.kyfex_uwu == nil then Datastore.kyfex_uwu = {} end <br>" +
            "--initialize the table if it doesn't exist<br><br>" +
            "Datastore.kyfex_uwu.globalValue = \"owo\"<br>" +
            "print(Datastore.kyfex_uwu.globalValue) --prints \"owo\"<br><br>" +
            "Datastore.kyfex_uwu[7] = true</div></div>"
    ]
});
addApi({
    title: "Create",
    description: "The Create API is your go-to whenever you need to create a new Minecraft object. Each of these methods " +
        "creates and returns a different object with the specified properties.",
    blocks: [
        {
            name: "itemStack",
            description: "Allowed properties: <span class=\"code\">item</span>, <span class=\"code\">count</span>",
            props: { properties: "table" },
            returns: "ItemStack",
            examples: [{
                    props: { properties: "{ item=\"minecraft:diamond\", count=10 }" },
                    description: "Creates a new ItemStack with the <span class=\"code\">item</span> and <span class=\"code\">" +
                        "count</span> specified. <span class=\"code\">count</span> is optional, and defaults to 1.",
                }, {
                    props: { properties: "\"empty\"" },
                    description: "Returns an itemstack with <span class=\"code\">count</span> = 1 and <span class=\"code\">" +
                        "item</span> = \"minecraft:air\"; an empty stack.",
                }],
        },
        {
            name: "blockPos",
            props: { x: "integer", y: "integer", z: "integer" },
            returns: "BlockPos",
            examples: [{
                    props: { x: "36", y: "65", z: "21" },
                    description: "Creates a block position (useful if you want to add a block to the world.)",
                }],
        },
        {
            name: "blockState",
            description: "Allowed properties: <span class=\"code\">block</span>, <span class=\"code\">properties</span>",
            props: { properties: "table" },
            returns: "BlockState",
            examples: [{
                    props: { properties: "{block=\"minecraft:oak_log\", properties={ axis=\"x\" }}" },
                    description: "Creates a block state (useful if you want to add a block to the world.)",
                }],
        },
        {
            name: "inventory",
            props: { size: "integer" },
            returns: "SimpleInventory",
            examples: [{
                    props: { size: "9" },
                    description: "Creates an inventory with the specified size.",
                }],
        },
        {
            name: "entity.item",
            props: { world: "World", x: "number", y: "number", z: "number", stack: "ItemStack", "optional [vx": "number",
                vy: "number", vz: "number] " },
            returns: "ItemEntity",
            examples: [{
                    props: { world: "world", x: "8.5", y: "65", z: "103", stack: "Create.itemStack({...})" },
                    description: "Creates and spawns an item in the world with the specified x, y, and z.",
                }, {
                    props: { world: "world", x: "8.5", y: "65", z: "103", stack: "Create.itemStack({...})", vx: "0", vy: "-3.1", vz: "0" },
                    description: "Creates and spawns an item in the world with the specified x, y, and z, and the specified" +
                        "vx, vy, and vz (velocity).",
                }],
        },
    ]
});
addApi({
    title: "Registry",
    description: "The Registry API is a table containing anything that has been registered in the game, like blocks, " +
        "items, potions, villager professions, cat variants, and more.",
    blocks: ["The Registry API is structured similar to a regular Lua table; you can access values within it but can't" +
            "set values in it. To access"]
});
//# sourceMappingURL=api.js.map