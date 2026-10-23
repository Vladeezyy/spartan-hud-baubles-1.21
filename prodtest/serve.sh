#!/bin/sh
# Runs the NeoForge server in the current dir until "Done" or a crash, then stops it. Output: server.out.
# Hang watchdog: a server.out that stops growing for $STALL seconds (default 90) before "Done" is a hung start ("HUNG");
# at most 10 min.
rm -f server.out
stall=${STALL:-90}
(
	start=$(date +%s); last_size=-1; last_change=$start
	until grep -qE 'Done \(|Crash report|Failed to start|Exception in server tick' server.out 2>/dev/null; do
		size=$(wc -c < server.out 2>/dev/null || echo 0); now=$(date +%s)
		if [ "$size" != "$last_size" ]; then last_size=$size; last_change=$now
		elif [ $((now - last_change)) -ge "$stall" ]; then echo "HUNG: the server wrote nothing for ${stall}s before it was up" >&2; break
		fi
		[ $((now - start)) -ge 600 ] && { echo "TIMEOUT: the server was not up in 10 min" >&2; break; }
		sleep 2
	done
	sleep 3; echo stop; sleep 30
) | sh run.sh nogui > server.out 2>&1
grep -E 'Done \(|Crash report|Failed to start' server.out | head -3
grep -E '/(ERROR|FATAL)\]' server.out | sed -E 's/^\[[^]]*\] //' | cut -c1-250 | sort | uniq -c | sort -rn | head -20
