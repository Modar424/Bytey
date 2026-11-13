"""Authoring helpers for the BYTEWISE content bank.

Every question is written ONCE with both languages side by side so that the
ids, levels, categories, concepts, difficulty, option order and correct index
cannot drift between the two JSON files.
"""
CATS = {"G": "GENERAL", "T": "TOKENS", "A": "AI_TOOLS", "M": "MODELS", "R": "ARCHITECTURE"}
RECORDS = []  # filled by qs()


def qs(level, cat_code, items):
    """items: list of (conceptId, difficulty, correctIndex, en, ar)
    en / ar = (question text, [4 options], explanation)."""
    cat = CATS[cat_code]
    assert len(items) == 5, f"level {level} {cat}: need exactly 5 questions, got {len(items)}"
    for n, (concept, diff, correct, en, ar) in enumerate(items, 1):
        RECORDS.append({
            "id": f"L{level:02d}_{cat_code}{n}", "level": level, "category": cat,
            "conceptId": concept, "difficulty": diff, "correctIndex": correct,
            "en": {"text": en[0], "options": list(en[1]), "explanation": en[2]},
            "ar": {"text": ar[0], "options": list(ar[1]), "explanation": ar[2]},
        })
