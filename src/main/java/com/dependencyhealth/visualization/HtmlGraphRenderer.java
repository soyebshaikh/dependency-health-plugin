package com.dependencyhealth.visualization;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HtmlGraphRenderer {

    /**
     * Generates a self-contained HTML file wrapping D3.js and the exported JSON graph.
     */
    public void renderHtml(String graphJson, File outputFile) throws IOException {
        String htmlTemplate = "<!DOCTYPE html>\n" +
"<html lang=\"en\">\n" +
"<head>\n" +
"    <meta charset=\"UTF-8\">\n" +
"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
"    <title>Dependency Risk Graph</title>\n" +
"    <script src=\"https://d3js.org/d3.v7.min.js\"></script>\n" +
"    <style>\n" +
"        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #121212; color: #ffffff; margin: 0; padding: 0; overflow: hidden; }\n" +
"        #legend { position: absolute; top: 10px; left: 10px; background: rgba(30,30,30,0.8); padding: 15px; border-radius: 8px; border: 1px solid #333; }\n" +
"        .legend-item { display: flex; align-items: center; margin-bottom: 5px; font-size: 14px; }\n" +
"        .color-box { width: 16px; height: 16px; margin-right: 10px; border-radius: 3px; }\n" +
"        .tooltip { position: absolute; background: #fff; color: #000; padding: 10px; border-radius: 4px; pointer-events: none; font-size: 13px; display: none; box-shadow: 0 4px 6px rgba(0,0,0,0.3); z-index: 1000; }\n" +
"        \n" +
"        svg { width: 100vw; height: 100vh; }\n" +
"        .link { fill: none; stroke-opacity: 0.6; }\n" +
"        \n" +
"        .node circle.SAFE { fill: #4CAF50; }\n" +
"        .node circle.LOW { fill: #2196F3; }\n" +
"        .node circle.MEDIUM { fill: #FFEB3B; }\n" +
"        .node circle.HIGH { fill: #FF9800; }\n" +
"        .node circle.CRITICAL { fill: #F44336; }\n" +
"        \n" +
"        .color-box.SAFE { background-color: #4CAF50; }\n" +
"        .color-box.LOW { background-color: #2196F3; }\n" +
"        .color-box.MEDIUM { background-color: #FFEB3B; }\n" +
"        .color-box.HIGH { background-color: #FF9800; }\n" +
"        .color-box.CRITICAL { background-color: #F44336; }\n" +
"        \n" +
"        .blast-radius { stroke: #F44336 !important; stroke-width: 3px !important; stroke-opacity: 0.8 !important; }\n" +
"        \n" +
"        .node text { pointer-events: none; font-size: 10px; fill: #eee; text-anchor: middle; }\n" +
"    </style>\n" +
"</head>\n" +
"<body>\n" +
"    <div id=\"author-label\" style=\"position: absolute; bottom: 10px; right: 10px; font-size: 14px; font-weight: bold; color: rgba(255,255,255,0.5);\">Created by Soyeb</div>\n" +
"    <div id=\"legend\">\n" +
"        <b>Risk Legend</b>\n" +
"        <div class=\"legend-item\"><div class=\"color-box SAFE\"></div>Safe / Unknown</div>\n" +
"        <div class=\"legend-item\"><div class=\"color-box LOW\"></div>Low Risk</div>\n" +
"        <div class=\"legend-item\"><div class=\"color-box MEDIUM\"></div>Medium Risk</div>\n" +
"        <div class=\"legend-item\"><div class=\"color-box HIGH\"></div>High Risk</div>\n" +
"        <div class=\"legend-item\"><div class=\"color-box CRITICAL\"></div>Critical Risk</div>\n" +
"        <br>\n" +
"        <div class=\"legend-item\"><div class=\"color-box\" style=\"background:#F44336; height:3px; align-self:center;\"></div>Blast Radius Path</div>\n" +
"    </div>\n" +
"    \n" +
"    <div class=\"tooltip\" id=\"tooltip\"></div>\n" +
"    <svg></svg>\n" +
"\n" +
"    <script>\n" +
"        const rawData = {{GRAPH_JSON}};\n" +
"\n" +
"        const width = window.innerWidth;\n" +
"        const height = window.innerHeight;\n" +
"\n" +
"        const svg = d3.select(\"svg\")\n" +
"            .call(d3.zoom().on(\"zoom\", function(event) {\n" +
"                container.attr(\"transform\", event.transform);\n" +
"            }))\n" +
"            .append(\"g\");\n" +
"            \n" +
"        const container = svg.append(\"g\");\n" +
"\n" +
"        // SVG Markers for arrows\n" +
"        svg.append(\"defs\").selectAll(\"marker\")\n" +
"            .data([\"normal\", \"blast\"])\n" +
"            .enter().append(\"marker\")\n" +
"            .attr(\"id\", d => d)\n" +
"            .attr(\"viewBox\", \"0 -5 10 10\")\n" +
"            .attr(\"refX\", 15)\n" +
"            .attr(\"refY\", 0)\n" +
"            .attr(\"markerWidth\", 6)\n" +
"            .attr(\"markerHeight\", 6)\n" +
"            .attr(\"orient\", \"auto\")\n" +
"            .append(\"path\")\n" +
"            .attr(\"d\", \"M0,-5L10,0L0,5\")\n" +
"            .attr(\"fill\", d => d === 'blast' ? '#F44336' : '#999');\n" +
"\n" +
"        const simulation = d3.forceSimulation(rawData.nodes)\n" +
"            .force(\"link\", d3.forceLink(rawData.links).id(d => d.id).distance(80))\n" +
"            .force(\"charge\", d3.forceManyBody().strength(-300))\n" +
"            .force(\"center\", d3.forceCenter(width / 2, height / 2))\n" +
"            .force(\"collide\", d3.forceCollide().radius(20));\n" +
"\n" +
"        const link = container.append(\"g\")\n" +
"            .selectAll(\"path\")\n" +
"            .data(rawData.links)\n" +
"            .join(\"path\")\n" +
"            .attr(\"class\", d => `link ${d.type === 'blast_radius' ? 'blast-radius' : ''}`)\n" +
"            .attr(\"stroke\", d => d.type === 'blast_radius' ? '#F44336' : '#999')\n" +
"            .attr(\"stroke-width\", d => d.type === 'blast_radius' ? 3 : 1)\n" +
"            .attr(\"marker-end\", d => d.type === 'blast_radius' ? 'url(#blast)' : 'url(#normal)');\n" +
"\n" +
"        const node = container.append(\"g\")\n" +
"            .selectAll(\"g\")\n" +
"            .data(rawData.nodes)\n" +
"            .join(\"g\")\n" +
"            .attr(\"class\", \"node\")\n" +
"            .call(drag(simulation));\n" +
"\n" +
"        node.append(\"circle\")\n" +
"            .attr(\"r\", d => d.isDirect ? 12 : 8)\n" +
"            .attr(\"class\", d => d.risk)\n" +
"            .attr(\"stroke\", \"#fff\")\n" +
"            .attr(\"stroke-width\", 1.5)\n" +
"            .on(\"mouseover\", showTooltip)\n" +
"            .on(\"mouseout\", hideTooltip);\n" +
"\n" +
"        node.append(\"text\")\n" +
"            .attr(\"dy\", -15)\n" +
"            .text(d => d.name);\n" +
"\n" +
"        simulation.on(\"tick\", () => {\n" +
"            link.attr(\"d\", d => {\n" +
"                const dx = d.target.x - d.source.x,\n" +
"                      dy = d.target.y - d.source.y,\n" +
"                      dr = Math.sqrt(dx * dx + dy * dy);\n" +
"                \n" +
"                // Keep straight lines for dependency trees instead of arcs for clarity\n" +
"                return `M${d.source.x},${d.source.y}L${d.target.x},${d.target.y}`;\n" +
"            });\n" +
"\n" +
"            node.attr(\"transform\", d => `translate(${d.x},${d.y})`);\n" +
"        });\n" +
"\n" +
"        // Tooltip logic\n" +
"        const tooltip = d3.select(\"#tooltip\");\n" +
"        function showTooltip(event, d) {\n" +
"            let metaHtml = \"\";\n" +
"            if (d.metadata) {\n" +
"                if (d.metadata.isEol) metaHtml += \"<br><b>EOL:</b> Yes\";\n" +
"                if (d.metadata.vulnerabilitiesCount > 0) metaHtml += `<br><b>CVEs:</b> ${d.metadata.vulnerabilitiesCount}`;\n" +
"                if (d.metadata.notes) metaHtml += `<br><b>Notes:</b> ${d.metadata.notes}`;\n" +
"            }\n" +
"            \n" +
"            tooltip.style(\"display\", \"block\")\n" +
"                   .html(`<b>${d.id}</b><br>Risk: <span style=\"font-weight:bold\" class=\"${d.risk}\">${d.risk}</span>${metaHtml}`)\n" +
"                   .style(\"left\", (event.pageX + 15) + \"px\")\n" +
"                   .style(\"top\", (event.pageY - 10) + \"px\");\n" +
"                   \n" +
"            d3.select(this).attr(\"stroke\", \"#000\").attr(\"stroke-width\", 2);\n" +
"        }\n" +
"        \n" +
"        function hideTooltip() {\n" +
"            tooltip.style(\"display\", \"none\");\n" +
"            d3.select(this).attr(\"stroke\", \"#fff\").attr(\"stroke-width\", 1.5);\n" +
"        }\n" +
"\n" +
"        // Drag functions\n" +
"        function drag(simulation) {\n" +
"            function dragstarted(event) {\n" +
"                if (!event.active) simulation.alphaTarget(0.3).restart();\n" +
"                event.subject.fx = event.subject.x;\n" +
"                event.subject.fy = event.subject.y;\n" +
"            }\n" +
"            function dragged(event) {\n" +
"                event.subject.fx = event.x;\n" +
"                event.subject.fy = event.y;\n" +
"            }\n" +
"            function dragended(event) {\n" +
"                if (!event.active) simulation.alphaTarget(0);\n" +
"                event.subject.fx = null;\n" +
"                event.subject.fy = null;\n" +
"            }\n" +
"            return d3.drag()\n" +
"                     .on(\"start\", dragstarted)\n" +
"                     .on(\"drag\", dragged)\n" +
"                     .on(\"end\", dragended);\n" +
"        }\n" +
"    </script>\n" +
"</body>\n" +
"</html>";

        String finalHtml = htmlTemplate.replace("{{GRAPH_JSON}}", graphJson);

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(finalHtml);
        }
    }
}
