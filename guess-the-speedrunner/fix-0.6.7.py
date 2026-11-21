from pathlib import Path

p=Path("src/main/java/dev/zunserz/gts/server/DeathrunTriggers.java")
s=p.read_text(encoding="utf-8")
mark="                    // Magenta/white checkpoint rings indicate activation radius and centre.\n"
start=s.index(mark)
old="            ShowData.Point a=FIRST.get(p.getUuid()),b=SECOND.get(p.getUuid());"
end=s.index(old,start)
block=s[start:end]
assert block.count("drawCheckpoint(world,p,cp,checkpointBudget)") == 1
s=s[:start]+s[end:]
needle=old+"\n            if(a!=null && b!=null && dim.equals(a.dimension)"
assert needle in s
s=s.replace(needle,block+needle,1)
s=s.replace('                case "zone" -> {\n'+old,'                case "zone" -> {\n                    ShowData.Point a=FIRST.get(p.getUuid()),b=SECOND.get(p.getUuid());',1)
assert s.count("checkpointBudget=300")==1
p.write_text(s,encoding="utf-8")
print("Checkpoint marker moved to CODER render tick")
