package parser.command;

import parser.Action;
import parser.Command;
import parser.Response;
import util.ListMakerHelper;
import util.Registration;
import util.mixin.InventoryMixin;
import world.Item;
import world.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


// TODO: "take _ from _" functionality
// TODO: "take" -> "Take what?"
// DONE: "take all"



public class TakeCommand extends Command {

    public TakeCommand( boolean register, String id, List<String> names) {
        super(register, id, null, names);
    }

    public TakeCommand(String id, List<String> names) {
        this(true, id, names);
    }

    public TakeCommand(String id, String... names) {
        this(id, new ArrayList<String>(Arrays.asList(names)));
    }

    @Override
    public Response getResponse(String playerInput) {
        TakeResponse returnResponse = new TakeResponse(100);

        String object = playerInput;
        returnResponse.initialize(object);

        return returnResponse;
    }


    private static class TakeResponse extends Response {
        public TakeResponse(int severity) {
            super("", severity);
        }

        private String object = null;
        private Item[] things = null;
        private Item[] roomthings = null;
        private Item[] nonstaticroomthings = null;
        private Item[] yourthings = null;

        protected void initialize(String object) {
            this.object = object;
            if(object.equals("all")) {
                things = null;
            } else {
                things = Registration.<Item>searchOwnerByStr("item_name", object).toArray(new Item[]{});
            }
        }

        private static enum ItemPresence {
            SUCCESS,
            STATIC,
            NOTPRESENT,     //can include present in container nearby
            FAILURE,
            ALL
        }

        private ItemPresence getResult(Player player) {
            if (this.object.equals("all")) {
                return ItemPresence.ALL;
            }
            if(things.length == 0) {
                return ItemPresence.FAILURE;
            }

            ArrayList<Item> roomthings_ = new ArrayList<>();
            ArrayList<Item> yourthings_ = new ArrayList<>();
            ArrayList<Item> nonstaticroomthings_ = new ArrayList<>();
            for(Item thing : things) {
                if (player.getRoom().getInventoryList().contains(thing)) {
                    roomthings_.add(thing);
                    if(! thing.getStatic()) {
                        nonstaticroomthings_.add(thing);
                    }
                }
                if (player.getInventoryList().contains(thing)) {
                    yourthings_.add(thing);
                }
            }
            yourthings = yourthings_.toArray(new Item[]{});
            roomthings = roomthings_.toArray(new Item[]{});
            nonstaticroomthings = nonstaticroomthings_.toArray(new Item[]{});

            if(roomthings_.size() == 0) {
                return ItemPresence.NOTPRESENT;
            }
            if(nonstaticroomthings.length == 0) {
                return ItemPresence.STATIC;
            }
            return ItemPresence.SUCCESS;
        }

        @Override
        public String getPlayerMessage(Player player) {
            switch(getResult(player)) {
                case ALL:
                    List<Item> invl = player.getRoom().getInventoryList();
                    for(java.util.Iterator<Item> iter = invl.iterator(); iter.hasNext();) {
                        if(iter.next().getStatic()) {
                            iter.remove();
                        }
                    }
                    Item[] inv = invl.toArray(new Item[]{});
                    if(inv.length == 0) {
                        return "You don't see much worth taking.";
                    } else {
                        StringBuilder itemsDescription = new StringBuilder();
                        itemsDescription.append("You pick up ");

                        ListMakerHelper help = new ListMakerHelper(inv.length, ".\n");
                        for(int i = 0; i < inv.length; ++i ) {
                            itemsDescription.append(inv[i].getArticle());
                            itemsDescription.append(" ");
                            itemsDescription.append(inv[i].getMixin("primaryname").get());
                            itemsDescription.append(help.getNextSeparator());
                        }
                        return itemsDescription.toString();
                    }
                case SUCCESS:
                    return "You pick up the " + roomthings[0].getPrimaryName() + ".";
                case STATIC:
                    return "It doesn't seem as though anything like that could be picked up.";
                case NOTPRESENT:
                    if(yourthings.length != 0) {
                        return "You already have that.";
                    }
                case FAILURE:
                default:
                    return "You don't see anything like that nearby.";
            }
        }

        @Override
        public ArrayList<Action> getActions(Player player) {
            ArrayList<Action> actions = new ArrayList<>();
            if(getResult(player) == ItemPresence.SUCCESS) {
                actions.add( (Player p) -> {
                    p.getRoom().getInventoryMixin().remove(nonstaticroomthings[0]);
                    p.getInventoryMixin().add(nonstaticroomthings[0]);
                });
            } else if(getResult(player) == ItemPresence.ALL) {
                actions.add( (Player p) -> {
                    InventoryMixin<?> roomInvMix = p.getRoom().getInventoryMixin();
                    InventoryMixin<?> playInvMix = p.getInventoryMixin();
                    Item[] inv = roomInvMix.get();
                    for(Item item : inv) {
                        if(! item.getStatic()) {
                            roomInvMix.remove(item);
                            playInvMix.add(item);
                        }
                    }
                });
            }
            return actions;
        }
    }

}
