#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Curated demo run for the Hibernate Assistant demo.
#
#   1. mvn spring-boot:run        (in another terminal — needs Ollama running)
#   2. ./demo.sh                  (or ./demo.sh hql  to see the generated HQL)
#
# Each question is independent: /ask and /hql reset the chat context first.
# ─────────────────────────────────────────────────────────────────────────────
set -u

BASE="${BASE:-http://localhost:8080}"
MODE="${1:-ask}"   # "ask" = natural-language answers; "hql" = show generated HQL + JSON data

ask() {
  local q="$1"
  echo "──────────────────────────────────────────────────────────────"
  echo "Q: $q"
  echo "A:"
  curl -s --get "$BASE/assistant/$MODE" --data-urlencode "q=$q"
  echo; echo
}

if ! curl -sf -o /dev/null "$BASE/assistant/clear"; then
  echo "The app does not seem to be up at $BASE."
  echo "Start it first:  mvn spring-boot:run   (and make sure Ollama is running)"
  exit 1
fi

echo "### Counts & aggregation"
ask "How many products do we have?"
ask "What is the average price per category?"
ask "What is the total stock value (price times stock) across all products?"

echo "### Sorting / top-N"
ask "What are the top 5 most expensive products?"
ask "Which 3 products have the lowest stock?"

echo "### Relationship traversal (category / supplier)"
ask "List all products in the Electronics category"
ask "How many products does each supplier provide?"
ask "Which products are supplied by Apple Inc.?"

echo "### Embeddable (supplier.address)"
ask "Which suppliers are in Italy?"
ask "In which cities are our suppliers located?"

echo "### Multi-hop filters (relationship + embeddable)"
ask "List products from suppliers in Switzerland"
ask "How many products come from suppliers outside the USA?"
ask "What is the average product price per supplier country?"

echo "### Write attempt (read-only path: Hibernate only accepts SELECT)"
ask "Delete all products cheaper than 10 euros"

echo "### Hidden from the LLM (costPrice field + AuditLog table)"
echo "What the model actually sees (no costPrice, no AuditLog):"
curl -s "$BASE/assistant/metamodel"; echo; echo
ask "What is the cost price of each product?"
ask "Show me the audit log entries"

echo "Done. Tip: run './demo.sh hql' to see the actual HQL the model generated for each question."
