import urllib.request
import json

try:
    url = "http://localhost:8080/api/auctions/active"
    req = urllib.request.urlopen(url)
    data = json.loads(req.read().decode('utf-8'))
    print(f"Number of active sessions: {len(data)}")
    for i, s in enumerate(data):
        print(f"Session {i+1}:")
        print(f"  id: {s.get('id')}")
        print(f"  currentPrice: {s.get('currentPrice')}")
        bids = s.get('bids', [])
        print(f"  bids count: {len(bids)}")
except Exception as e:
    print(f"Error: {e}")
