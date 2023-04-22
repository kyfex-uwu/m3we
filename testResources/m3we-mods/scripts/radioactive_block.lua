function onUse(state,world,pos,player,hand,hit)
    world.setBlockState(pos,
            state.with(Properties.get(state,"density"),
                    (state.get(Properties.get(state,"density"))+1)%4
            )
    )
end

function returnLuminance(state)
    return state.get(Properties.get(state,"density"))*4-1
end