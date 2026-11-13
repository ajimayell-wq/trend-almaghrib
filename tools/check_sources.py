"""Teste chaque source de Sources.kt avec l'identifiant de l'application et avec un navigateur."""
import re, ssl, urllib.request, urllib.error, email.utils, datetime

src = open("app/src/main/java/ma/trend/almaghrib/engine/Sources.kt", encoding="utf-8").read()
urls = []
for m in re.finditer(r'Source\("([^"]+)", "[^"]*", "[^"]+", "[^"]+",\s*"([^"]+)"', src):
    urls.append((m.group(1), m.group(2)))
for m in re.finditer(r'yt\("([^"]+)", "[^"]*", "([^"]+)"', src):
    urls.append((m.group(1), "https://www.youtube.com/feeds/videos.xml?channel_id=" + m.group(2)))

UAS = {
    "app": "TrendAlMaghrib/1.0 (Android)",
    "chrome": "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36",
}
now = datetime.datetime.now(datetime.timezone.utc)


def parse(d):
    d = d.strip()
    try:
        return email.utils.parsedate_to_datetime(d)
    except Exception:
        pass
    try:
        return datetime.datetime.fromisoformat(d.replace("Z", "+00:00"))
    except Exception:
        return None


def newest(body):
    dates = [parse(d) for d in re.findall(r"<(?:pubDate|published|updated)>([^<]+)</", body)]
    dates = [d for d in dates if d is not None and d.tzinfo is not None]
    return max(dates) if dates else None


def probe(url, ua):
    req = urllib.request.Request(url, headers={"User-Agent": ua})
    try:
        with urllib.request.urlopen(req, timeout=20, context=ssl.create_default_context()) as r:
            body = r.read().decode("utf-8", "replace")
            n = len(re.findall(r"<item[\s>]", body)) + len(re.findall(r"<entry[\s>]", body))
            nw = newest(body)
            age = f"il y a {(now - nw).total_seconds() / 3600:.0f} h" if nw else "sans date lisible"
            kind = "XML" if body.lstrip().startswith("<?xml") or "<rss" in body[:500] or "<feed" in body[:500] else "PAS XML"
            return f"{r.status} {kind}, {n} elements, plus recent {age}"
    except urllib.error.HTTPError as e:
        return f"HTTP {e.code}"
    except Exception as e:
        return f"ERREUR {type(e).__name__}: {str(e)[:80]}"


for sid, url in urls:
    print(f"{sid:16} app: {probe(url, UAS['app'])}")
    print(f"{'':16} chrome: {probe(url, UAS['chrome'])}")
