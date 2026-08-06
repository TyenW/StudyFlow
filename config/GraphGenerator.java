package config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class GraphGenerator {

    public static void generate(Path vaultDir, List<Path> files, Map<Path, FrontmatterParser.NoteMetadata> metadataMap) throws IOException {
        Path htmlFile = vaultDir.resolve("segundo_cerebro_grafo.html");

        // Prepare lists for JSON
        StringBuilder nodesBuilder = new StringBuilder();
        StringBuilder edgesBuilder = new StringBuilder();

        // Sets to prevent duplicates
        Set<String> addedTags = new HashSet<>();
        Set<String> addedEdges = new HashSet<>(); // format: "from_to"

        // Map from file names/aliases to node ID (the file name without extension)
        Map<String, String> linkTargetToNodeId = new HashMap<>();

        for (Path file : files) {
            String relativePath = vaultDir.relativize(file).toString();
            String nameNoExt = getBaseName(file);
            linkTargetToNodeId.put(nameNoExt.toLowerCase(), nameNoExt);
            
            FrontmatterParser.NoteMetadata meta = metadataMap.get(file);
            if (meta != null) {
                for (String alias : meta.aliases) {
                    linkTargetToNodeId.put(alias.toLowerCase(), nameNoExt);
                }
            }
        }

        // Generate Nodes
        int nodeCount = 0;
        for (Path file : files) {
            String nameNoExt = getBaseName(file);
            String folderName = file.getParent() != null ? file.getParent().getFileName().toString() : "Raiz";
            FrontmatterParser.NoteMetadata meta = metadataMap.get(file);

            String tagsList = "";
            String aliasesList = "";
            String criado = "";
            String atualizado = "";

            if (meta != null) {
                tagsList = String.join(", ", meta.tags);
                aliasesList = String.join(", ", meta.aliases);
                criado = meta.criado_em;
                atualizado = meta.atualizado_em;
            }

            if (nodeCount > 0) nodesBuilder.append(",\n");
            nodesBuilder.append(String.format(
                "  {id: \"%s\", label: \"%s\", group: \"note\", folder: \"%s\", tags: \"%s\", aliases: \"%s\", criado: \"%s\", atualizado: \"%s\"}",
                escapeJs(nameNoExt), escapeJs(nameNoExt), escapeJs(folderName), escapeJs(tagsList), escapeJs(aliasesList), escapeJs(criado), escapeJs(atualizado)
            ));
            nodeCount++;

            // Create tag nodes and connect them
            if (meta != null) {
                for (String tag : meta.tags) {
                    String cleanTag = tag.trim().toLowerCase();
                    if (cleanTag.isEmpty()) continue;

                    // Add tag node
                    if (!addedTags.contains(cleanTag)) {
                        nodesBuilder.append(",\n");
                        nodesBuilder.append(String.format(
                            "  {id: \"tag_%s\", label: \"#%s\", group: \"tag\", folder: \"Tags\", tags: \"\", aliases: \"\", criado: \"\", atualizado: \"\"}",
                            escapeJs(cleanTag), escapeJs(cleanTag)
                        ));
                        addedTags.add(cleanTag);
                    }

                    // Add edge from note to tag
                    String edgeKey = nameNoExt + "_tag_" + cleanTag;
                    if (!addedEdges.contains(edgeKey)) {
                        if (edgesBuilder.length() > 0) edgesBuilder.append(",\n");
                        edgesBuilder.append(String.format(
                            "  {from: \"%s\", to: \"tag_%s\", color: {color: \"#a855f7\", opacity: 0.4}, length: 150}",
                            escapeJs(nameNoExt), escapeJs(cleanTag)
                        ));
                        addedEdges.add(edgeKey);
                    }
                }

                // Parse content for wikilinks and add edges between notes
                List<String> wikilinks = extractWikilinks(meta.content);
                for (String link : wikilinks) {
                    String targetNodeId = linkTargetToNodeId.get(link.toLowerCase());
                    if (targetNodeId != null && !targetNodeId.equals(nameNoExt)) {
                        String edgeKey = nameNoExt + "_" + targetNodeId;
                        String edgeKeyRev = targetNodeId + "_" + nameNoExt;
                        if (!addedEdges.contains(edgeKey) && !addedEdges.contains(edgeKeyRev)) {
                            if (edgesBuilder.length() > 0) edgesBuilder.append(",\n");
                            edgesBuilder.append(String.format(
                                "  {from: \"%s\", to: \"%s\", color: {color: \"#3b82f6\", opacity: 0.6}, length: 250}",
                                escapeJs(nameNoExt), escapeJs(targetNodeId)
                            ));
                            addedEdges.add(edgeKey);
                        }
                    }
                }
            }
        }

        String htmlTemplate = getHtmlTemplate(nodesBuilder.toString(), edgesBuilder.toString());
        Files.writeString(htmlFile, htmlTemplate, StandardCharsets.UTF_8);
    }

    private static String getBaseName(Path path) {
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        return idx == -1 ? name : name.substring(0, idx);
    }

    private static List<String> extractWikilinks(String content) {
        List<String> links = new ArrayList<>();
        if (content == null) return links;

        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String link = matcher.group(1).trim();
            // Handle pipe aliases like [[Note Name|Alias]]
            if (link.contains("|")) {
                link = link.substring(0, link.indexOf("|")).trim();
            }
            if (!link.isEmpty() && !link.endsWith(".png") && !link.endsWith(".jpg") && !link.endsWith(".jpeg") && !link.endsWith(".pdf")) {
                links.add(link);
            }
        }
        return links;
    }

    private static String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private static String getHtmlTemplate(String nodesJson, String edgesJson) {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"pt-BR\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <title>Segundo Cérebro - Grafo de Notas</title>\n" +
               "    <script type=\"text/javascript\" src=\"https://unpkg.com/vis-network/standalone/umd/vis-network.min.js\"></script>\n" +
               "    <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap\">\n" +
               "    <style>\n" +
               "        * {\n" +
               "            box-sizing: border-box;\n" +
               "            margin: 0;\n" +
               "            padding: 0;\n" +
               "            font-family: 'Inter', sans-serif;\n" +
               "        }\n" +
               "        body, html {\n" +
               "            width: 100%;\n" +
               "            height: 100%;\n" +
               "            overflow: hidden;\n" +
               "            background-color: #0b0f19;\n" +
               "            color: #f3f4f6;\n" +
               "        }\n" +
               "        #app-layout {\n" +
               "            display: flex;\n" +
               "            width: 100%;\n" +
               "            height: 100%;\n" +
               "        }\n" +
               "        #sidebar {\n" +
               "            width: 380px;\n" +
               "            height: 100%;\n" +
               "            background: rgba(17, 24, 39, 0.7);\n" +
               "            backdrop-filter: blur(16px);\n" +
               "            -webkit-backdrop-filter: blur(16px);\n" +
               "            border-right: 1px solid rgba(255, 255, 255, 0.08);\n" +
               "            display: flex;\n" +
               "            flex-direction: column;\n" +
               "            z-index: 10;\n" +
               "            padding: 24px;\n" +
               "            overflow-y: auto;\n" +
               "        }\n" +
               "        #network-container {\n" +
               "            flex-grow: 1;\n" +
               "            height: 100%;\n" +
               "            position: relative;\n" +
               "        }\n" +
               "        h1 {\n" +
               "            font-size: 20px;\n" +
               "            font-weight: 700;\n" +
               "            margin-bottom: 20px;\n" +
               "            background: linear-gradient(135deg, #3b82f6, #a855f7);\n" +
               "            -webkit-background-clip: text;\n" +
               "            -webkit-text-fill-color: transparent;\n" +
               "            display: flex;\n" +
               "            align-items: center;\n" +
               "            gap: 10px;\n" +
               "        }\n" +
               "        .search-box {\n" +
               "            width: 100%;\n" +
               "            padding: 12px 16px;\n" +
               "            background: rgba(31, 41, 55, 0.5);\n" +
               "            border: 1px solid rgba(255, 255, 255, 0.1);\n" +
               "            border-radius: 8px;\n" +
               "            color: white;\n" +
               "            outline: none;\n" +
               "            margin-bottom: 20px;\n" +
               "            transition: border 0.2s;\n" +
               "        }\n" +
               "        .search-box:focus {\n" +
               "            border-color: #3b82f6;\n" +
               "        }\n" +
               "        .card {\n" +
               "            background: rgba(255, 255, 255, 0.03);\n" +
               "            border: 1px solid rgba(255, 255, 255, 0.05);\n" +
               "            border-radius: 12px;\n" +
               "            padding: 20px;\n" +
               "            margin-bottom: 20px;\n" +
               "            display: none;\n" +
               "        }\n" +
               "        .card.active {\n" +
               "            display: block;\n" +
               "            animation: fadeIn 0.3s ease;\n" +
               "        }\n" +
               "        @keyframes fadeIn {\n" +
               "            from { opacity: 0; transform: translateY(5px); }\n" +
               "            to { opacity: 1; transform: translateY(0); }\n" +
               "        }\n" +
               "        .card h2 {\n" +
               "            font-size: 16px;\n" +
               "            font-weight: 600;\n" +
               "            color: #3b82f6;\n" +
               "            margin-bottom: 12px;\n" +
               "        }\n" +
               "        .meta-group {\n" +
               "            margin-bottom: 12px;\n" +
               "        }\n" +
               "        .meta-label {\n" +
               "            font-size: 11px;\n" +
               "            color: #9ca3af;\n" +
               "            text-transform: uppercase;\n" +
               "            letter-spacing: 0.05em;\n" +
               "            margin-bottom: 4px;\n" +
               "        }\n" +
               "        .meta-value {\n" +
               "            font-size: 13px;\n" +
               "            font-weight: 500;\n" +
               "        }\n" +
               "        .tag-pill {\n" +
               "            display: inline-block;\n" +
               "            padding: 4px 10px;\n" +
               "            background: rgba(168, 85, 247, 0.15);\n" +
               "            color: #c084fc;\n" +
               "            border: 1px solid rgba(168, 85, 247, 0.3);\n" +
               "            border-radius: 999px;\n" +
               "            font-size: 11px;\n" +
               "            margin-right: 6px;\n" +
               "            margin-bottom: 6px;\n" +
               "        }\n" +
               "        .control-group {\n" +
               "            margin-top: auto;\n" +
               "            border-top: 1px solid rgba(255, 255, 255, 0.08);\n" +
               "            padding-top: 20px;\n" +
               "        }\n" +
               "        .control-item {\n" +
               "            display: flex;\n" +
               "            align-items: center;\n" +
               "            justify-content: space-between;\n" +
               "            margin-bottom: 14px;\n" +
               "            font-size: 13px;\n" +
               "        }\n" +
               "        /* Toggle Switch Styling */\n" +
               "        .switch {\n" +
               "            position: relative;\n" +
               "            display: inline-block;\n" +
               "            width: 36px;\n" +
               "            height: 20px;\n" +
               "        }\n" +
               "        .switch input {\n" +
               "            opacity: 0;\n" +
               "            width: 0;\n" +
               "            height: 0;\n" +
               "        }\n" +
               "        .slider {\n" +
               "            position: absolute;\n" +
               "            cursor: pointer;\n" +
               "            top: 0; left: 0; right: 0; bottom: 0;\n" +
               "            background-color: #374151;\n" +
               "            transition: .3s;\n" +
               "            border-radius: 20px;\n" +
               "        }\n" +
               "        .slider:before {\n" +
               "            position: absolute;\n" +
               "            content: \"\";\n" +
               "            height: 14px; width: 14px;\n" +
               "            left: 3px; bottom: 3px;\n" +
               "            background-color: white;\n" +
               "            transition: .3s;\n" +
               "            border-radius: 50%;\n" +
               "        }\n" +
               "        input:checked + .slider {\n" +
               "            background-color: #3b82f6;\n" +
               "        }\n" +
               "        input:checked + .slider:before {\n" +
               "            transform: translateX(16px);\n" +
               "        }\n" +
               "        .placeholder-text {\n" +
               "            color: #6b7280;\n" +
               "            font-size: 13px;\n" +
               "            text-align: center;\n" +
               "            margin-top: 40px;\n" +
               "            line-height: 1.5;\n" +
               "        }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div id=\"app-layout\">\n" +
               "        <div id=\"sidebar\">\n" +
               "            <h1>🧠 Segundo Cérebro</h1>\n" +
               "            <input type=\"text\" id=\"search\" class=\"search-box\" placeholder=\"Pesquisar nota ou tag...\">\n" +
               "\n" +
               "            <div id=\"info-placeholder\" class=\"placeholder-text\">\n" +
               "                Clique em um nó para visualizar seus metadados e conexões no cofre.\n" +
               "            </div>\n" +
               "\n" +
               "            <div id=\"details-card\" class=\"card\">\n" +
               "                <h2 id=\"node-title\">Título da Nota</h2>\n" +
               "                \n" +
               "                <div class=\"meta-group\">\n" +
               "                    <div class=\"meta-label\">Tipo</div>\n" +
               "                    <div id=\"node-type\" class=\"meta-value\">-</div>\n" +
               "                </div>\n" +
               "                <div class=\"meta-group\">\n" +
               "                    <div class=\"meta-label\">Pasta</div>\n" +
               "                    <div id=\"node-folder\" class=\"meta-value\">-</div>\n" +
               "                </div>\n" +
               "                <div id=\"group-criado\" class=\"meta-group\">\n" +
               "                    <div class=\"meta-label\">Criado em</div>\n" +
               "                    <div id=\"node-criado\" class=\"meta-value\">-</div>\n" +
               "                </div>\n" +
               "                <div id=\"group-atualizado\" class=\"meta-group\">\n" +
               "                    <div class=\"meta-label\">Atualizado em</div>\n" +
               "                    <div id=\"node-atualizado\" class=\"meta-value\">-</div>\n" +
               "                </div>\n" +
               "                <div id=\"group-aliases\" class=\"meta-group\">\n" +
               "                    <div class=\"meta-label\">Apelidos (Aliases)</div>\n" +
               "                    <div id=\"node-aliases\" class=\"meta-value\">-</div>\n" +
               "                </div>\n" +
               "                <div id=\"group-tags\" class=\"meta-group\">\n" +
               "                    <div class=\"meta-label\">Tags</div>\n" +
               "                    <div id=\"node-tags\"></div>\n" +
               "                </div>\n" +
               "            </div>\n" +
               "\n" +
               "            <div class=\"control-group\">\n" +
               "                <div class=\"control-item\">\n" +
               "                    <span>Exibir tags como nós</span>\n" +
               "                    <label class=\"switch\">\n" +
               "                        <input type=\"checkbox\" id=\"toggle-tags\" checked>\n" +
               "                        <span class=\"slider\"></span>\n" +
               "                    </label>\n" +
               "                </div>\n" +
               "            </div>\n" +
               "        </div>\n" +
               "        <div id=\"network-container\"></div>\n" +
               "    </div>\n" +
               "\n" +
               "    <script>\n" +
               "        const rawNodes = [\n" + nodesJson + "\n" +
               "        ];\n" +
               "        const rawEdges = [\n" + edgesJson + "\n" +
               "        ];\n" +
               "\n" +
               "        let network = null;\n" +
               "        let nodesDataSet = new vis.DataSet();\n" +
               "        let edgesDataSet = new vis.DataSet();\n" +
               "\n" +
               "        function initNetwork() {\n" +
               "            const showTags = document.getElementById('toggle-tags').checked;\n" +
               "            \n" +
               "            // Filter nodes\n" +
               "            const filteredNodes = rawNodes.filter(n => showTags || n.group !== 'tag');\n" +
               "            const allowedNodeIds = new Set(filteredNodes.map(n => n.id));\n" +
               "            \n" +
               "            // Filter edges\n" +
               "            const filteredEdges = rawEdges.filter(e => allowedNodeIds.has(e.from) && allowedNodeIds.has(e.to));\n" +
               "\n" +
               "            nodesDataSet.clear();\n" +
               "            edgesDataSet.clear();\n" +
               "            \n" +
               "            // Style groups\n" +
               "            const styledNodes = filteredNodes.map(n => {\n" +
               "                if (n.group === 'note') {\n" +
               "                    return {\n" +
               "                        ...n,\n" +
               "                        shape: 'dot',\n" +
               "                        size: 16,\n" +
               "                        color: {\n" +
               "                            background: '#1e3a8a',\n" +
               "                            border: '#3b82f6',\n" +
               "                            highlight: { background: '#2563eb', border: '#60a5fa' }\n" +
               "                        },\n" +
               "                        font: { color: '#e5e7eb', size: 12, face: 'Inter' }\n" +
               "                    };\n" +
               "                } else {\n" +
               "                    return {\n" +
               "                        ...n,\n" +
               "                        shape: 'box',\n" +
               "                        size: 10,\n" +
               "                        color: {\n" +
               "                            background: '#3b0764',\n" +
               "                            border: '#a855f7',\n" +
               "                            highlight: { background: '#581c87', border: '#c084fc' }\n" +
               "                        },\n" +
               "                        font: { color: '#c084fc', size: 10, face: 'Inter' },\n" +
               "                        margin: 8\n" +
               "                    };\n" +
               "                }\n" +
               "            });\n" +
               "\n" +
               "            nodesDataSet.add(styledNodes);\n" +
               "            edgesDataSet.add(filteredEdges);\n" +
               "\n" +
               "            const container = document.getElementById('network-container');\n" +
               "            const data = { nodes: nodesDataSet, edges: edgesDataSet };\n" +
               "            \n" +
               "            const options = {\n" +
               "                physics: {\n" +
               "                    barnesHut: {\n" +
               "                        gravitationalConstant: -3000,\n" +
               "                        centralGravity: 0.1,\n" +
               "                        springLength: 200,\n" +
               "                        springConstant: 0.05,\n" +
               "                        damping: 0.09,\n" +
               "                        avoidOverlap: 0.1\n" +
               "                    },\n" +
               "                    stabilization: { iterations: 150 }\n" +
               "                },\n" +
               "                interaction: {\n" +
               "                    hover: true,\n" +
               "                    tooltipDelay: 200\n" +
               "                }\n" +
               "            };\n" +
               "\n" +
               "            network = new vis.Network(container, data, options);\n" +
               "\n" +
               "            // Event Listeners\n" +
               "            network.on('click', function(params) {\n" +
               "                if (params.nodes.length > 0) {\n" +
               "                    const nodeId = params.nodes[0];\n" +
               "                    const nodeData = nodesDataSet.get(nodeId);\n" +
               "                    showNodeDetails(nodeData);\n" +
               "                } else {\n" +
               "                    hideNodeDetails();\n" +
               "                }\n" +
               "            });\n" +
               "        }\n" +
               "\n" +
               "        function showNodeDetails(node) {\n" +
               "            document.getElementById('info-placeholder').style.display = 'none';\n" +
               "            const card = document.getElementById('details-card');\n" +
               "            card.classList.add('active');\n" +
               "\n" +
               "            document.getElementById('node-title').textContent = node.label;\n" +
               "            document.getElementById('node-type').textContent = node.group === 'note' ? 'Nota de Estudo' : 'Tag';\n" +
               "            document.getElementById('node-folder').textContent = node.folder;\n" +
               "            \n" +
               "            if (node.group === 'note') {\n" +
               "                document.getElementById('group-criado').style.display = 'block';\n" +
               "                document.getElementById('group-atualizado').style.display = 'block';\n" +
               "                document.getElementById('node-criado').textContent = node.criado || 'Não informada';\n" +
               "                document.getElementById('node-atualizado').textContent = node.atualizado || 'Não informada';\n" +
               "                \n" +
               "                if (node.aliases) {\n" +
               "                    document.getElementById('group-aliases').style.display = 'block';\n" +
               "                    document.getElementById('node-aliases').textContent = node.aliases;\n" +
               "                } else {\n" +
               "                    document.getElementById('group-aliases').style.display = 'none';\n" +
               "                }\n" +
               "\n" +
               "                const tagContainer = document.getElementById('node-tags');\n" +
               "                tagContainer.innerHTML = '';\n" +
               "                if (node.tags) {\n" +
               "                    document.getElementById('group-tags').style.display = 'block';\n" +
               "                    node.tags.split(',').forEach(tag => {\n" +
               "                        const trimmed = tag.trim();\n" +
               "                        if (trimmed) {\n" +
               "                            const pill = document.createElement('span');\n" +
               "                            pill.className = 'tag-pill';\n" +
               "                            pill.textContent = '#' + trimmed;\n" +
               "                            tagContainer.appendChild(pill);\n" +
               "                        }\n" +
               "                    });\n" +
               "                } else {\n" +
               "                    document.getElementById('group-tags').style.display = 'none';\n" +
               "                }\n" +
               "            } else {\n" +
               "                // Hide note-only groups for tags\n" +
               "                document.getElementById('group-criado').style.display = 'none';\n" +
               "                document.getElementById('group-atualizado').style.display = 'none';\n" +
               "                document.getElementById('group-aliases').style.display = 'none';\n" +
               "                document.getElementById('group-tags').style.display = 'none';\n" +
               "            }\n" +
               "        }\n" +
               "\n" +
               "        function hideNodeDetails() {\n" +
               "            document.getElementById('details-card').classList.remove('active');\n" +
               "            document.getElementById('info-placeholder').style.display = 'block';\n" +
               "        }\n" +
               "\n" +
               "        // Search filter logic\n" +
               "        document.getElementById('search').addEventListener('input', function(e) {\n" +
               "            const query = e.target.value.toLowerCase().trim();\n" +
               "            if (!query) {\n" +
               "                // Clear highlights\n" +
               "                nodesDataSet.forEach(node => {\n" +
               "                    nodesDataSet.update({ id: node.id, hidden: false });\n" +
               "                });\n" +
               "                return;\n" +
               "            }\n" +
               "            \n" +
               "            nodesDataSet.forEach(node => {\n" +
               "                const matchesLabel = node.label.toLowerCase().includes(query);\n" +
               "                const matchesFolder = node.folder.toLowerCase().includes(query);\n" +
               "                const matchesTags = node.tags.toLowerCase().includes(query);\n" +
               "                const matchesAliases = node.aliases.toLowerCase().includes(query);\n" +
               "                \n" +
               "                const match = matchesLabel || matchesFolder || matchesTags || matchesAliases;\n" +
               "                nodesDataSet.update({ id: node.id, hidden: !match });\n" +
               "            });\n" +
               "        });\n" +
               "\n" +
               "        document.getElementById('toggle-tags').addEventListener('change', initNetwork);\n" +
               "\n" +
               "        // Init on load\n" +
               "        window.addEventListener('load', initNetwork);\n" +
               "    </script>\n" +
               "</body>\n" +
               "</html>";
    }
}
