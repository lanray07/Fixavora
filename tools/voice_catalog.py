"""Draft command dictionaries; speech engine language support must be tested on-device."""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
keys = "DUE_WEEK OVERDUE EXPENSES WARRANTIES COMPLETE APPOINTMENT".split()
phrases = {
"en-GB": "due this week|overdue|my expenses|warranty|mark completed|add an appointment",
"fr-FR": "cette semaine|en retard|mes dépenses|garantie|marquer comme terminé|ajouter un rendez-vous",
"es-ES": "esta semana|atrasadas|mis gastos|garantía|marcar como completada|añadir una cita",
"de-DE": "diese woche|überfällig|meine ausgaben|garantie|als erledigt markieren|termin hinzufügen",
"it-IT": "questa settimana|in ritardo|le mie spese|garanzia|segna come completato|aggiungi appuntamento",
"pt-BR": "esta semana|atrasadas|minhas despesas|garantia|marcar como concluída|adicionar compromisso",
"pt-PT": "esta semana|em atraso|as minhas despesas|garantia|marcar como concluída|adicionar compromisso",
"nl-NL": "deze week|achterstallig|mijn uitgaven|garantie|markeer als voltooid|afspraak toevoegen",
"pl-PL": "w tym tygodniu|zaległe|moje wydatki|gwarancja|oznacz jako ukończone|dodaj wizytę",
"sv-SE": "denna vecka|försenade|mina utgifter|garanti|markera som klar|lägg till bokning",
"ja-JP": "今週|期限切れ|費用|保証|完了にする|予約を追加",
"hi-IN": "इस सप्ताह|बकाया|मेरे खर्च|वारंटी|पूर्ण चिह्नित|मुलाकात जोड़ें",
"ar": "هذا الأسبوع|المتأخرة|مصروفاتي|الضمان|وضع علامة مكتمل|إضافة موعد",
"tr-TR": "bu hafta|gecikmiş|giderlerim|garanti|tamamlandı olarak işaretle|randevu ekle",
"id-ID": "minggu ini|terlambat|pengeluaran saya|garansi|tandai selesai|tambah janji"
}
for target, source in {"en-US":"en-GB", "en-AU":"en-GB", "fr-CA":"fr-FR", "es-419":"es-ES"}.items(): phrases[target] = phrases[source]
data = {tag: {k:[v] for k,v in zip(keys, values.split("|"))} for tag,values in phrases.items()}
data["en-GB"]["COMPLETE"] += ["mark", "complete task"]
data["en-GB"]["EXPENSES"] += ["repair expenses", "my spending"]
data["en-GB"]["APPOINTMENT"] += ["contractor appointment"]
out = ROOT / "app/src/main/assets/voice-commands.json"; out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
assert len(data) == 19 and all(len(v)==6 for v in data.values())
print("PASS: 19 draft dictionaries, six intents each; acoustic engine support NOT TESTED")
