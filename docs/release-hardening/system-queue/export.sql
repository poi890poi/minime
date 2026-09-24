WITH main AS (
  SELECT DISTINCT t.utid, t.upid
  FROM slice s
  JOIN thread_track tt ON s.track_id=tt.id
  JOIN thread t ON tt.utid=t.utid
  JOIN process p ON t.upid=p.upid
  WHERE s.name GLOB 'MinIME.decoder.callback.*'
    AND p.name='app.minime.keyboard' AND t.tid=p.pid
)
SELECT 'main' AS kind, utid AS id, 0 AS ts, 0 AS dur, '' AS name, upid AS aux FROM main
UNION ALL
SELECT 'queue', s.id, s.ts, s.dur, s.name, pt.upid
FROM slice s JOIN process_track pt ON s.track_id=pt.id
WHERE pt.upid IN (SELECT upid FROM main) AND s.name GLOB 'MinIME.decoder.queue.*'
UNION ALL
SELECT 'callback', s.id, s.ts, s.dur, s.name, tt.utid
FROM slice s JOIN thread_track tt ON s.track_id=tt.id
WHERE tt.utid IN (SELECT utid FROM main) AND s.name GLOB 'MinIME.decoder.callback.*'
UNION ALL
SELECT 'state', id, ts, dur, state, utid FROM thread_state
WHERE utid IN (SELECT utid FROM main)
UNION ALL
SELECT 'frame', s.id, s.ts, s.dur, 'Choreographer#doFrame', tt.utid
FROM slice s JOIN thread_track tt ON s.track_id=tt.id
WHERE tt.utid IN (SELECT utid FROM main) AND s.name GLOB 'Choreographer#doFrame*'
UNION ALL
SELECT 'health', idx, 0, value, name, severity FROM stats
WHERE value>0 AND (severity IN ('data_loss','error') OR name GLOB '*discard*' OR name GLOB '*overrun*');
