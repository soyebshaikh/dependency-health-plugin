package com.dependencyhealth.visualization;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HtmlGraphRenderer {

    /**
     * Generates a self-contained HTML file wrapping D3.js and the exported JSON graph.
     */
    public void renderHtml(String graphJson, File outputFile) throws IOException {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>Dependency Risk Graph</title>\n");
        html.append("  <script src=\"https://d3js.org/d3.v7.min.js\"></script>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #121212; color: #fff; margin: 0; padding: 0; overflow: hidden; }\n");
        html.append("    svg { width: 100vw; height: 100vh; }\n");

        // Links
        html.append("    .link { fill: none; stroke-opacity: 0.6; }\n");
        html.append("    .link-dimmed    { stroke-opacity: 0.05 !important; }\n");
        html.append("    .link-connected { stroke-opacity: 0.75 !important; stroke-width: 2px !important; }\n");

        // Nodes
        html.append("    .node text { pointer-events: none; font-size: 10px; fill: #eee; text-anchor: middle; }\n");
        html.append("    .node-dimmed { opacity: 0.1 !important; }\n");
        html.append("    .node circle.SAFE     { fill: #4CAF50; }\n");
        html.append("    .node circle.DUPLICATE{ fill: #8b5cf6; }\n");
        html.append("    .node circle.LOW      { fill: #2196F3; }\n");
        html.append("    .node circle.MEDIUM   { fill: #FFEB3B; }\n");
        html.append("    .node circle.HIGH     { fill: #FF9800; }\n");
        html.append("    .node circle.CRITICAL { fill: #F44336; }\n");

        // Highlight modes
        html.append("    .highlight-duplicate { stroke: #a78bfa !important; stroke-width: 4px !important; stroke-dasharray: 4; animation: pulse-violet 2s infinite; }\n");
        html.append("    @keyframes pulse-violet { 0%{fill:#8b5cf6;} 50%{fill:#a78bfa;} 100%{fill:#8b5cf6;} }\n");
        html.append("    .highlight-search { stroke: #facc15 !important; stroke-width: 3px !important; }\n");
        html.append("    .blast-radius { stroke: #F44336 !important; stroke-width: 3px !important; stroke-opacity: 0.85 !important; }\n");

        // Tooltip
        html.append("    .tooltip { position: absolute; background: #fff; color: #000; padding: 10px; border-radius: 4px; pointer-events: none; font-size: 13px; display: none; box-shadow: 0 4px 8px rgba(0,0,0,0.4); z-index: 1000; max-width: 280px; }\n");

        // Legend
        html.append("    #legend { position: absolute; top: 10px; left: 10px; background: rgba(20,20,30,0.9); padding: 14px; border-radius: 8px; border: 1px solid #333; z-index: 10; }\n");
        html.append("    .legend-item { display: flex; align-items: center; margin-bottom: 5px; font-size: 13px; }\n");
        html.append("    .color-box { width: 14px; height: 14px; margin-right: 9px; border-radius: 3px; flex-shrink: 0; }\n");
        html.append("    .color-box.SAFE      { background:#4CAF50; }\n");
        html.append("    .color-box.DUPLICATE { background:#8b5cf6; }\n");
        html.append("    .color-box.LOW       { background:#2196F3; }\n");
        html.append("    .color-box.MEDIUM    { background:#FFEB3B; }\n");
        html.append("    .color-box.HIGH      { background:#FF9800; }\n");
        html.append("    .color-box.CRITICAL  { background:#F44336; }\n");

        // Tools panel
        html.append("    #tools-panel { position: absolute; top: 10px; right: 10px; background: rgba(20,20,30,0.92); padding: 15px; border-radius: 8px; border: 1px solid #3d3d5c; width: 275px; z-index: 10; }\n");
        html.append("    .panel-title { font-weight: 700; font-size: 12px; letter-spacing: 0.06em; margin-bottom: 13px; color: #c4b5fd; text-transform: uppercase; }\n");

        // Search
        html.append("    .search-section { margin-bottom: 13px; }\n");
        html.append("    .search-label { font-size: 11px; color: #888; margin-bottom: 6px; display: block; }\n");
        html.append("    .search-row { display: flex; gap: 6px; }\n");
        html.append("    #search-input { flex: 1; background: #1a1a2e; color: #fff; border: 1px solid #444; border-radius: 4px; padding: 6px 8px; font-size: 12px; outline: none; transition: border 0.2s; }\n");
        html.append("    #search-input:focus { border-color: #8b5cf6; }\n");
        html.append("    #search-input::placeholder { color: #555; }\n");
        html.append("    #search-results { margin-top: 8px; max-height: 145px; overflow-y: auto; display: none; }\n");
        html.append("    .search-result-item { padding: 7px 8px; border-bottom: 1px solid #2a2a3e; font-size: 12px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; transition: background 0.15s; }\n");
        html.append("    .search-result-item:hover { background: rgba(139,92,246,0.2); }\n");
        html.append("    .search-result-name { color: #e2e8f0; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 160px; }\n");
        html.append("    .search-result-version { color: #a78bfa; font-size: 10px; background: rgba(139,92,246,0.18); padding: 2px 6px; border-radius: 3px; white-space: nowrap; }\n");

        // Buttons
        html.append("    .btn { background: #8b5cf6; color: #fff; border: none; padding: 7px 10px; border-radius: 4px; cursor: pointer; font-size: 12px; font-weight: 600; transition: background 0.2s; white-space: nowrap; }\n");
        html.append("    .btn:hover { background: #7c3aed; }\n");
        html.append("    .btn-full { width: 100%; margin-bottom: 7px; display: block; }\n");
        html.append("    .btn-reset { background: #2d3748; }\n");
        html.append("    .btn-reset:hover { background: #4a5568; }\n");
        html.append("    .divider { border: none; border-top: 1px solid #2a2a3e; margin: 12px 0; }\n");

        // No-result helper
        html.append("    .no-results { color: #555; font-size: 11px; text-align: center; padding: 6px 0; }\n");

        // Duplicates list
        html.append("    #duplicates-list { margin-top: 10px; max-height: 250px; overflow-y: auto; display: none; }\n");
        html.append("    .dup-item { padding: 9px; border-bottom: 1px solid #2a2a3e; }\n");
        html.append("    .dup-name { font-weight: bold; color: #a78bfa; font-size: 12px; margin-bottom: 5px; }\n");
        html.append("    .dup-versions { display: flex; flex-wrap: wrap; gap: 4px; }\n");
        html.append("    .dup-version-tag { font-size: 10px; color: #e2e8f0; background: #252540; padding: 2px 7px; border-radius: 3px; cursor: pointer; border: 1px solid #444; transition: all 0.2s; }\n");
        html.append("    .dup-version-tag:hover { border-color: #8b5cf6; color: #a78bfa; }\n");

        // Author
        html.append("    #author-label { position: absolute; bottom: 10px; right: 10px; font-size: 13px; color: rgba(255,255,255,0.35); }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div id=\"author-label\">Created by Soyeb</div>\n");

        // Tools panel HTML
        html.append("  <div id=\"tools-panel\">\n");
        html.append("    <div class=\"panel-title\">Graph Tools</div>\n");
        html.append("    <div class=\"search-section\">\n");
        html.append("      <span class=\"search-label\">Search by name or version</span>\n");
        html.append("      <div class=\"search-row\">\n");
        html.append("        <input id=\"search-input\" type=\"text\" placeholder=\"e.g. jackson, 2.15.2 ...\" oninput=\"onSearchInput(this.value)\" />\n");
        html.append("        <button class=\"btn\" onclick=\"pinSearch()\">Pin</button>\n");
        html.append("      </div>\n");
        html.append("      <div id=\"search-results\"></div>\n");
        html.append("    </div>\n");
        html.append("    <hr class=\"divider\">\n");
        html.append("    <button class=\"btn btn-full\" onclick=\"findDuplicates()\">Find Duplicate Versions</button>\n");
        html.append("    <button class=\"btn btn-full btn-reset\" onclick=\"resetGraph()\">Reset / Clear</button>\n");
        html.append("    <div id=\"duplicates-list\"></div>\n");
        html.append("  </div>\n");

        // Legend
        html.append("  <div id=\"legend\">\n");
        html.append("    <b>Risk Legend</b>\n");
        html.append("    <div class=\"legend-item\"><div class=\"color-box SAFE\"></div>Safe / Unknown</div>\n");
        html.append("    <div class=\"legend-item\"><div class=\"color-box DUPLICATE\"></div>Duplicate</div>\n");
        html.append("    <div class=\"legend-item\"><div class=\"color-box LOW\"></div>Low Risk</div>\n");
        html.append("    <div class=\"legend-item\"><div class=\"color-box MEDIUM\"></div>Medium Risk</div>\n");
        html.append("    <div class=\"legend-item\"><div class=\"color-box HIGH\"></div>High Risk</div>\n");
        html.append("    <div class=\"legend-item\"><div class=\"color-box CRITICAL\"></div>Critical Risk</div>\n");
        html.append("    <br>\n");
        html.append("    <div class=\"legend-item\"><div class=\"color-box\" style=\"background:#F44336;height:3px;align-self:center;\"></div>Blast Radius Path</div>\n");
        html.append("  </div>\n");

        html.append("  <div class=\"tooltip\" id=\"tooltip\"></div>\n");
        html.append("  <svg></svg>\n");

        // JavaScript
        html.append("  <script>\n");
        html.append("    const rawData = ").append(graphJson).append(";\n");
        html.append("""
const width  = window.innerWidth;
const height = window.innerHeight;

// ── SVG / zoom setup ────────────────────────────────────────────────────────
const svgRoot = d3.select('svg');
const zoomBehavior = d3.zoom().on('zoom', (event) => container.attr('transform', event.transform));
svgRoot.call(zoomBehavior);
const svg       = svgRoot.append('g');
const container = svg.append('g');

// Arrow markers
svg.append('defs').selectAll('marker')
    .data(['normal', 'blast'])
    .enter().append('marker')
    .attr('id', d => d)
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 15).attr('refY', 0)
    .attr('markerWidth', 6).attr('markerHeight', 6)
    .attr('orient', 'auto')
    .append('path')
    .attr('d', 'M0,-5L10,0L0,5')
    .attr('fill', d => d === 'blast' ? '#F44336' : '#999');

// ── Force simulation ─────────────────────────────────────────────────────────
const simulation = d3.forceSimulation(rawData.nodes)
    .force('link',    d3.forceLink(rawData.links).id(d => d.id).distance(80))
    .force('charge',  d3.forceManyBody().strength(-300))
    .force('center',  d3.forceCenter(width / 2, height / 2))
    .force('collide', d3.forceCollide().radius(20));

// ── Links ────────────────────────────────────────────────────────────────────
const link = container.append('g')
    .selectAll('path')
    .data(rawData.links)
    .join('path')
    .attr('class', d => 'link' + (d.type === 'blast_radius' ? ' blast-radius' : ''))
    .attr('stroke',       d => d.type === 'blast_radius' ? '#F44336' : '#999')
    .attr('stroke-width', d => d.type === 'blast_radius' ? 3 : 1)
    .attr('marker-end',   d => d.type === 'blast_radius' ? 'url(#blast)' : 'url(#normal)');

// ── Nodes ────────────────────────────────────────────────────────────────────
const node = container.append('g')
    .selectAll('g')
    .data(rawData.nodes)
    .join('g')
    .attr('class', 'node')
    .call(drag(simulation));

node.append('circle')
    .attr('r', d => d.isDirect ? 12 : 8)
    .attr('class', d => d.risk)
    .attr('stroke', '#fff')
    .attr('stroke-width', 1.5)
    .on('mouseover', showTooltip)
    .on('mouseout',  hideTooltip);

node.append('text')
    .attr('dy', -15)
    .text(d => d.name);

simulation.on('tick', () => {
    link.attr('d', d => `M${d.source.x},${d.source.y}L${d.target.x},${d.target.y}`);
    node.attr('transform', d => `translate(${d.x},${d.y})`);
});

// ── Tooltip ──────────────────────────────────────────────────────────────────
const tooltip = d3.select('#tooltip');
function showTooltip(event, d) {
    let meta = '';
    if (d.metadata) {
        if (d.metadata.isEol) meta += '<br><b>EOL:</b> Yes';
        if (d.metadata.vulnerabilitiesCount > 0) meta += `<br><b>CVEs:</b> ${d.metadata.vulnerabilitiesCount}`;
        if (d.metadata.notes) meta += `<br><b>Notes:</b> ${d.metadata.notes}`;
    }
    const ver = d.version ? `<br><b>Version:</b> ${d.version}` : '';
    tooltip.style('display', 'block')
           .html(`<b>${d.id}</b>${ver}<br>Risk: <span style="font-weight:bold">${d.risk}</span>${meta}`)
           .style('left', (event.pageX + 15) + 'px')
           .style('top',  (event.pageY - 10) + 'px');
    d3.select(this).attr('stroke', '#000').attr('stroke-width', 2);
}
function hideTooltip() {
    tooltip.style('display', 'none');
    d3.select(this).attr('stroke', '#fff').attr('stroke-width', 1.5);
}

// ── Reset ─────────────────────────────────────────────────────────────────────
function resetGraph() {
    node.classed('node-dimmed', false);
    node.selectAll('circle')
        .classed('highlight-duplicate', false)
        .classed('highlight-search',    false);
    link.classed('link-dimmed',    false)
        .classed('link-connected', false);
    document.getElementById('duplicates-list').style.display = 'none';
    document.getElementById('duplicates-list').innerHTML     = '';
    document.getElementById('search-input').value            = '';
    document.getElementById('search-results').style.display  = 'none';
    document.getElementById('search-results').innerHTML      = '';
}

// ── applyFocus ────────────────────────────────────────────────────────────────
// Dims non-active nodes; keeps links that touch active nodes clearly visible.
function applyFocus(activeIds, mode) {
    const connectedLinkIdxs = new Set();
    rawData.links.forEach((l, i) => {
        const src = l.source.id !== undefined ? l.source.id : l.source;
        const tgt = l.target.id !== undefined ? l.target.id : l.target;
        if (activeIds.has(src) || activeIds.has(tgt)) connectedLinkIdxs.add(i);
    });

    node.classed('node-dimmed', d => !activeIds.has(d.id));
    node.selectAll('circle')
        .classed('highlight-duplicate', d => mode === 'duplicate' && activeIds.has(d.id))
        .classed('highlight-search',    d => mode === 'search'    && activeIds.has(d.id));

    link.each(function(d, i) {
        const connected = connectedLinkIdxs.has(i);
        d3.select(this)
            .classed('link-dimmed',    !connected)
            .classed('link-connected',  connected);
    });
}

// ── Search ────────────────────────────────────────────────────────────────────
function onSearchInput(query) {
    const q = query.trim().toLowerCase();
    const resultsEl = document.getElementById('search-results');
    if (!q) { resetGraph(); return; }

    const matched = rawData.nodes.filter(n =>
        (n.name     && n.name.toLowerCase().includes(q))    ||
        (n.id       && n.id.toLowerCase().includes(q))      ||
        (n.version  && n.version.toLowerCase().includes(q)) ||
        (n.metadata && n.metadata.artifactId && n.metadata.artifactId.toLowerCase().includes(q))
    );

    applyFocus(new Set(matched.map(n => n.id)), 'search');

    resultsEl.innerHTML = '';
    if (matched.length === 0) {
        resultsEl.style.display = 'block';
        resultsEl.innerHTML = '<div class="no-results">No matches found</div>';
        return;
    }
    resultsEl.style.display = 'block';
    matched.slice(0, 20).forEach(n => {
        const item = document.createElement('div');
        item.className = 'search-result-item';
        const artName = (n.metadata && n.metadata.artifactId) ? n.metadata.artifactId : (n.name || n.id);
        const ver     = n.version || '';
        item.innerHTML = `<span class="search-result-name">${artName}</span><span class="search-result-version">${ver}</span>`;
        item.onclick   = () => focusOnNode(n.id);
        resultsEl.appendChild(item);
    });
    if (matched.length > 20) {
        const more = document.createElement('div');
        more.className   = 'no-results';
        more.textContent = `… ${matched.length - 20} more`;
        resultsEl.appendChild(more);
    }
}

// Pin: zoom into the best matching node
function pinSearch() {
    const q = document.getElementById('search-input').value.trim().toLowerCase();
    if (!q) return;
    const match = rawData.nodes.find(n =>
        (n.name    && n.name.toLowerCase().includes(q))    ||
        (n.id      && n.id.toLowerCase().includes(q))      ||
        (n.version && n.version.toLowerCase() === q)
    );
    if (match) focusOnNode(match.id);
}

// ── Duplicate Detection ───────────────────────────────────────────────────────
function findDuplicates() {
    const groups = {};
    rawData.nodes.forEach(n => {
        // Primary: use metadata (always set now for all nodes)
        // Fallback: parse from id which is "groupId:artifactId:version"
        let gId, aId;
        if (n.metadata && n.metadata.groupId) {
            gId = n.metadata.groupId;
            aId = n.metadata.artifactId;
        } else if (n.id && n.id.split(':').length >= 3) {
            const parts = n.id.split(':');
            gId = parts[0];
            aId = parts[1];
        }
        if (gId && aId) {
            const key = `${gId}:${aId}`;
            if (!groups[key]) groups[key] = [];
            groups[key].push(n);
        }
    });

    const dupKeys    = Object.keys(groups).filter(k => groups[k].length > 1);
    const dupNodeIds = new Set();
    dupKeys.forEach(k => groups[k].forEach(n => dupNodeIds.add(n.id)));

    const listEl = document.getElementById('duplicates-list');
    listEl.innerHTML = '';

    if (dupKeys.length === 0) {
        listEl.style.display = 'block';
        listEl.innerHTML = '<div class="no-results">No duplicate versions found.</div>';
        return;
    }

    // Focus graph: highlight duplicates, keep their edges bright
    applyFocus(dupNodeIds, 'duplicate');

    listEl.style.display = 'block';
    listEl.innerHTML = `<div style="font-size:11px;color:#777;margin-bottom:8px">${dupKeys.length} conflict(s) found — click a version tag to zoom</div>`;
    dupKeys.forEach(k => {
        const item = document.createElement('div');
        item.className = 'dup-item';
        const artName = k.split(':')[1];
        const tags = groups[k]
            .map(n => `<span class="dup-version-tag" onclick="focusOnNode('${n.id}')">${n.version || n.id}</span>`)
            .join('');
        item.innerHTML = `<div class="dup-name">${artName}</div><div class="dup-versions">${tags}</div>`;
        listEl.appendChild(item);
    });
}

// ── Focus / zoom to a node ────────────────────────────────────────────────────
function focusOnNode(nodeId) {
    const n = rawData.nodes.find(x => x.id === nodeId);
    if (!n) return;
    const t = d3.zoomIdentity
        .translate(width / 2, height / 2)
        .scale(2.2)
        .translate(-n.x, -n.y);
    svgRoot.transition().duration(700).call(zoomBehavior.transform, t);
}

// ── Drag ──────────────────────────────────────────────────────────────────────
function drag(sim) {
    return d3.drag()
        .on('start', (event) => {
            if (!event.active) sim.alphaTarget(0.3).restart();
            event.subject.fx = event.subject.x;
            event.subject.fy = event.subject.y;
        })
        .on('drag', (event) => {
            event.subject.fx = event.x;
            event.subject.fy = event.y;
        })
        .on('end', (event) => {
            if (!event.active) sim.alphaTarget(0);
            event.subject.fx = null;
            event.subject.fy = null;
        });
}
""");
        html.append("  </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        String finalHtml = html.toString();

        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(finalHtml);
        }
    }
}
