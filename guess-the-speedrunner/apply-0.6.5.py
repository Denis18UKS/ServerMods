from pathlib import Path
p=Path('src/main/java/dev/zunserz/gts/server/ShowDirector.java')
s=p.read_text()
def rep(old,new):
 global s
 assert old in s, 'Missing target in ShowDirector: '+old[:80]
 s=s.replace(old,new,1)
rep('    public static void namesSync(MinecraftServer server){', '''    public static String tabLabel(String role,int number){
        String base=label(role,number);
        if(number>0 && (role.equals("ACTOR")||role.equals("SPEEDRUNNER"))
                && ShowStore.DATA.eliminated.contains("Игрок "+number))return base+" (Выбыл)";
        return base;
    }
    public static void namesSync(MinecraftServer server){''')
rep('b.writeUuid(p.getUuid());b.writeString(label(p),64);', '''b.writeUuid(p.getUuid());
                String role=FakeControl.effectiveRole(p);
                int number=FakeControl.isControlling(p)?FakeControl.controlledNumber(p):
                        ShowStore.DATA.numbers.getOrDefault(p.getUuidAsString(),0);
                b.writeString(tabLabel(role,number),64);''')
rep('String tag=label(FakePlayers.role(name),FakePlayers.number(name));',
    'String tag=tabLabel(FakePlayers.role(name),FakePlayers.number(name));')
rep('    public static void completeChallenge(MinecraftServer server,String id){', '''    /** DEV test bypass for discussion/voting only. Never skip unfinished trials. */
    public static boolean startNextForDev(ServerPlayerEntity player,String requested){
        if(!dev(player))return false;
        String next=nextChallenge();
        if(!ORDER.contains(requested) || !requested.equals(next)){
            player.sendMessage(Text.literal("§cПорядок Крафт → Дезран → Чанк; следующее: "+next),false);
            return false;
        }
        if(ShowStore.DATA.phase.equals(requested.toUpperCase(Locale.ROOT))){
            player.sendMessage(Text.literal("§eИспытание уже запущено"),false);
            return false;
        }
        stopTimer(player.getServer());
        ShowScheduler.reset();
        ShowStore.DATA.votingOpen=false;
        ShowStore.DATA.votes.clear();
        ShowStore.markDirty();
        boolean started=startNext(player);
        if(started)player.sendMessage(Text.literal("§aDEV: запущено "+requested),false);
        return started;
    }
    public static void completeChallenge(MinecraftServer server,String id){''')
rep('        ShowFlow.run(server,"vote_finished");ShowStore.markDirty();',
    '        ShowFlow.run(server,"vote_finished");ShowStore.markDirty();\n        namesSync(server);')
rep('        for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList())GuessShow.syncRole(p);',
    '        for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList())GuessShow.syncRole(p);\n        namesSync(server);')
p.write_text(s)
p=Path('src/main/java/dev/zunserz/gts/server/ShowCommandsExtended.java');s=p.read_text()
old='''            .then(literal("challenge")
                .then(literal("complete")'''
new='''            .then(literal("challenge")
                .then(literal("start").then(argument("id",StringArgumentType.word())
                    .suggests((c,b)->CommandSource.suggestMatching(ShowDirector.ORDER,b))
                    .executes(ctx->ShowDirector.startNextForDev(ctx.getSource().getPlayerOrThrow(),
                            StringArgumentType.getString(ctx,"id"))?1:0)))
                .then(literal("complete")'''
assert old in s;s=s.replace(old,new,1);p.write_text(s)
p=Path('src/main/java/dev/zunserz/gts/mixin/PlayerListHudMixin.java');s=p.read_text()
old='cir.setReturnValue(Text.literal(label));'
assert old in s;s=s.replace(old,'''cir.setReturnValue(label.endsWith(" (Выбыл)")
                ? Text.literal(label).formatted(Formatting.ITALIC) : Text.literal(label));''',1);p.write_text(s)
p=Path('gradle.properties');s=p.read_text();assert 'mod_version=0.6.4' in s;p.write_text(s.replace('mod_version=0.6.4','mod_version=0.6.5',1))
Path('CHANGELOG-0.6.5.md').write_text('''# GuessTheSpeedrunner 0.6.5

- Eliminated participants (real and virtual) remain in TAB as Игрок N (Выбыл), italic.
- DEV start command for Deathrun after Craft without waiting through discussion and voting: /showdev challenge start deathrun
- Challenge order is enforced. The regular show still waits for all voting phases.
- To test next step: /showdev challenge complete deathrun then /showdev challenge start chunk.
''',encoding='utf-8')
print('0.6.5 patch applied')
