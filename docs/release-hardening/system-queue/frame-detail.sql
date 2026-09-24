WITH main AS (
  SELECT DISTINCT t.utid FROM slice s
  JOIN thread_track tt ON s.track_id=tt.id
  JOIN thread t ON tt.utid=t.utid JOIN process p ON t.upid=p.upid
  WHERE s.name GLOB 'MinIME.decoder.callback.*' AND p.name='app.minime.keyboard' AND t.tid=p.pid
)
SELECT 'slice' AS kind,s.id,s.ts,s.dur,s.name,s.depth AS extra,s.parent_id AS related,'' AS detail
FROM slice s JOIN thread_track tt ON s.track_id=tt.id
WHERE tt.utid IN (SELECT utid FROM main)
UNION ALL
SELECT 'state',s.id,s.ts,s.dur,s.state,0,s.waker_utid,coalesce(w.name,'')
FROM thread_state s LEFT JOIN thread w ON w.utid=s.waker_utid
WHERE s.utid IN (SELECT utid FROM main);
