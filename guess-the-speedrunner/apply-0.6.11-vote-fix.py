from pathlib import Path
p=Path("src/main/java/dev/zunserz/gts/server/ShowEditor.java")
s=p.read_text()
old='if(!FakeControl.effectiveRole(p).equals("JURY") && !isSolo(p) && !ShowDirector.isGod(p))return;'
new='if(!FakeControl.effectiveRole(p).equals("JURY") && !isSolo(p) && !ShowDirector.dev(p))return;'
assert old in s
s=s.replace(old,new,1)
old='''            if(candidate.equals("Игрок "+index))player.changeGameMode(GameMode.SPECTATOR);
        }
    }'''
new='''            if(candidate.equals("Игрок "+index))player.changeGameMode(GameMode.SPECTATOR);
            if(FakeControl.effectiveRole(player).equals("JURY") || ShowDirector.dev(player))
                openVoting(player);
        }
    }'''
assert old in s
s=s.replace(old,new,1)
p.write_text(s)
p=Path("CHANGELOG-0.6.11.md")
s=p.read_text().replace("Single juror/solo testing resolves an elimination with one ballot.","Single juror/solo testing resolves an elimination with one ballot. DEV /coder can cast in tests; voters see updated grayed-out cards after expulsion.")
p.write_text(s)
print("DEV voting now server-authorized; ballot state refreshed")
