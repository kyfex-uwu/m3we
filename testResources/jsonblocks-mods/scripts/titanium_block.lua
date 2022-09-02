local timesClicked = 0;

function onUse(state,world,pos,player,hand,hit)
    local hungerLevel = player.hungerManager.getFoodLevel()-1
    if hungerLevel<0 then
        hungerLevel=0
    else
        timesClicked=timesClicked+1
        print("clicked "..timesClicked.." times")
        player.hungerManager.setFoodLevel(hungerLevel)
    end
end