from pathlib import Path
p=Path("src/main/java/dev/zunserz/gts/client/DeathrunEditorScreen.java")
s=p.read_text(encoding="utf-8")
needle='    @Override public void render(DrawContext dc,int mx,int my,float delta){'
addition='''    /** Always-drawn and directly clickable list, independent of widget visibility. */
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(button==0 && mouseX>=x0 && mouseX<x0+125){
            for(int row=0;row<6;row++){
                int at=page*6+row;
                if(at<results.size() && mouseY>=78+row*23 && mouseY<99+row*23){
                    selected=results.get(at);name.setText(selected);refresh();return true;
                }
            }
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }
'''
assert needle in s
s=s.replace(needle,addition+needle,1)
needle='        dc.drawCenteredTextWithShadow(textRenderer,"§dGTS · DEATHRUN CONFIGURATOR",width/2,10,0xFFFFFF);'
addition='''        // Explicitly draw the true library names even with hidden native buttons.
        for(int row=0;row<6;row++){
            int at=page*6+row;
            if(at>=results.size())break;
            String id=results.get(at);
            dc.drawTextWithShadow(textRenderer,(id.equals(selected)?"§a● ":"§7○ ")+id,x0+8,84+row*23,0xFFFFFF);
        }
'''
assert needle in s
s=s.replace(needle,needle+"\n"+addition,1)
p.write_text(s,encoding="utf-8")
print("Visible/clickable Deathrun library applied")
