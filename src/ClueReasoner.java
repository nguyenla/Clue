/**
 * ClueReasoner.java - project skeleton for a propositional reasoner
 * for the game of Clue.  Unimplemented portions have the comment "TO
 * BE IMPLEMENTED AS AN EXERCISE".  The reasoner does not include
 * knowledge of how many cards each player holds.  See
 * http://cs.gettysburg.edu/~tneller/nsf/clue/ for details.
 *
 * @author Todd Neller
 * @version 1.0
 *

Copyright (C) 2005 Todd Neller

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

Information about the GNU General Public License is available online at:
  http://www.gnu.org/licenses/
To receive a copy of the GNU General Public License, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
02111-1307, USA.

 */

import java.io.*;
import java.util.*;

public class ClueReasoner 
{
	private int numPlayers;
	private int playerNum;
	private int numCards;
	private SATSolver solver;    
	private String caseFile = "cf";
	private String[] players = {"sc", "mu", "wh", "gr", "pe", "pl"};
	private String[] suspects = {"mu", "pl", "gr", "pe", "sc", "wh"};
	private String[] weapons = {"kn", "ca", "re", "ro", "pi", "wr"};
	private String[] rooms = {"ha", "lo", "di", "ki", "ba", "co", "bi", "li", "st"};
	private String[] cards;

	public ClueReasoner()
	{
		numPlayers = players.length;

		// Initialize card info
		cards = new String[suspects.length + weapons.length + rooms.length];
		int i = 0;
		for (String card : suspects)
			cards[i++] = card;
		for (String card : weapons)
			cards[i++] = card;
		for (String card : rooms)
			cards[i++] = card;
		numCards = i;

		// Initialize solver
		solver = new SATSolver();
		addInitialClauses();
	}

	private int getPlayerNum(String player) 
	{
		if (player.equals(caseFile))
			return numPlayers;
		for (int i = 0; i < numPlayers; i++)
			if (player.equals(players[i]))
				return i;
		System.out.println("Illegal player: " + player);
		return -1;
	}

	private int getCardNum(String card)
	{
		for (int i = 0; i < numCards; i++)
			if (card.equals(cards[i]))
				return i;
		System.out.println("Illegal card: " + card);
		return -1;
	}

	private int getPairNum(String player, String card) 
	{
		return getPairNum(getPlayerNum(player), getCardNum(card));
	}

	private int getPairNum(int playerNum, int cardNum)
	{
		return playerNum * numCards + cardNum + 1;
	}    

	// Add initial clauses method
	public void addInitialClauses() 
	{   
		// Each card is in at least one place (including case file).
		for (int c = 0; c < numCards; c++) {
			int[] clause = new int[numPlayers + 1];
			for (int p = 0; p <= numPlayers; p++)
				clause[p] = getPairNum(p, c);
			solver.addClause(clause);
		}    

		// If a card is one place, it cannot be in another place.
		// For each card C and each pair of players P1 and P2, add a clause of the form (~C.P1 V ~C.P2) 
		// which means that either player P1 does not have card C or player P2 does not have card C
		for (int c = 0; c < numCards; c++) {
			for (int j = 0; j <= numPlayers - 1; j++) {
				for (int k = j+1; k <= numPlayers; k++) {
					int[] clause = new int[2];
					clause[0] = 0 - getPairNum(j, c);
					clause[1] = 0 - getPairNum(k, c);
					solver.addClause(clause);
				}
			}
		}

		// At least one card of each category is in the case file.
		int cf = numPlayers; // treat the case file as a special player with index = numPlayers
		int[] suspectClause = new int[numPlayers];
		for (int c = 0; c < numPlayers; c++) {
			int cardNum = getCardNum(suspects[c]);
			suspectClause[c] = getPairNum(cf, cardNum);
		}
		solver.addClause(suspectClause);

		int[] weaponClause = new int[weapons.length];
		for (int c = 0; c < weapons.length; c++) {
			int cardNum = getCardNum(weapons[c]);
			weaponClause[c] = getPairNum(cf, cardNum);
		}
		solver.addClause(weaponClause);


		int[] roomClause = new int[rooms.length];
		for (int c = 0; c < rooms.length; c++) {
			int cardNum = getCardNum(rooms[c]);
			roomClause[c] = getPairNum(cf, cardNum);
		}
		solver.addClause(roomClause);

		// No two cards in each category can both be in the case file.
		// In general, for each pairs of card C1 and C2 from the same categories, add a clause of the form
		// (~C1.PCF V ~C2.PCF), which means either Card 1 is not in case file or Card 2 is not in case file
		// Suspects cards
		for (int s1 = 0; s1 < suspects.length-1; s1++) {
			for (int s2 = s1 + 1; s2 < suspects.length; s2++) {
				int cardNum1 = getCardNum(suspects[s1]);
				int cardNum2 = getCardNum(suspects[s2]);
				int[] clause = new int[2];
				clause[0] = 0 - getPairNum(cf, cardNum1);
				clause[1] = 0 - getPairNum(cf, cardNum2);
				solver.addClause(clause);
			}
		}

		// Weapons cards
		for (int w1 = 0; w1 < weapons.length-1; w1++) {
			for (int w2 = w1 + 1; w2 < weapons.length; w2++) {
				int cardNum1 = getCardNum(weapons[w1]);
				int cardNum2 = getCardNum(weapons[w2]);
				int[] clause = new int[2];
				clause[0] = 0 - getPairNum(cf, cardNum1);
				clause[1] = 0 - getPairNum(cf, cardNum2);
				solver.addClause(clause);
			}
		}

		// Rooms cards
		for (int r1 = 0; r1 < rooms.length-1; r1++) {
			for (int r2 = r1 + 1; r2 < rooms.length; r2++) {
				int cardNum1 = getCardNum(rooms[r1]);
				int cardNum2 = getCardNum(rooms[r2]);
				int[] clause = new int[2];
				clause[0] = 0 - getPairNum(cf, cardNum1);
				clause[1] = 0 - getPairNum(cf, cardNum2);
				solver.addClause(clause);
			}
		}
	}

	// Add to the SATSolver the simple clauses representing the known possession of the cards
	public void hand(String player, String[] cards) 
	{
		playerNum = getPlayerNum(player);

		// For each card C, add the literal representing "The player owns card C" 
		for (int i = 0; i < cards.length; i++) {
			int cardNum = getCardNum(cards[i]);
			int[] clause = new int[1];
			clause[0] = getPairNum(playerNum, cardNum);
			solver.addClause(clause);
		}

		// Comment
		for (int i = 0; i < this.cards.length; i++) {
			String curCard = this.cards[i];
			boolean isInHand = false;
			for (int j = 0; j < cards.length; j++) {
				if (cards[j].equals(curCard)) {
					isInHand = true;
				}
			}

			if (!isInHand) {
				int cardNum = getCardNum(curCard);
				int[] clause = new int[1];
				clause[0] = 0 - getPairNum(playerNum, cardNum);
				solver.addClause(clause);
			}

		}
	}

	public void suggest(String suggester, String card1, String card2, 
			String card3, String refuter, String cardShown) 
	{
		if (refuter != null) {
			int playerNum = getPlayerNum(suggester);
			if (playerNum == numPlayers - 1) {
				playerNum = 0;
			}
			else {
				playerNum += 1;
			}
			int refNum = getPlayerNum(refuter);

			while (playerNum != refNum) {
				// current player is not the refuter, meaning that player does not have any of the 3 cards
				int[] clause1 = new int[1];
				clause1[0] = 0-getPairNum(playerNum, getCardNum(card1));
				solver.addClause(clause1);

				int[] clause2 = new int[1];
				clause2[0] = 0-getPairNum(playerNum, getCardNum(card2));
				solver.addClause(clause2);

				int[] clause3 = new int[1];
				clause3[0] = 0-getPairNum(playerNum, getCardNum(card3));
				solver.addClause(clause3);

				if (playerNum == numPlayers - 1) {
					playerNum = 0;
				}
				else {
					playerNum += 1;
				}
			}

			// The refuter
			if (cardShown != null) {
				int[] clause = new int[1];
				clause[0] = getPairNum(refNum, getCardNum(cardShown));
				solver.addClause(clause);
			}
			else {
				int[] clause = new int[3];
				clause[0] = getPairNum(refNum, getCardNum(card1));
				clause[1] = getPairNum(refNum, getCardNum(card2));
				clause[2] = getPairNum(refNum, getCardNum(card3));
				solver.addClause(clause);
			}
		}
		else { // if the refuter is null
			// Each of the 3 cards suggested must be either with the suggester or in the case file
			int[] clause1 = new int[2];
			clause1[0] = getPairNum(getPlayerNum(suggester), getCardNum(card1));
			clause1[1] = getPairNum(numPlayers, getCardNum(card1)); // case file
			solver.addClause(clause1);

			int[] clause2 = new int[2];
			clause2[0] = getPairNum(getPlayerNum(suggester), getCardNum(card2));
			clause2[1] = getPairNum(numPlayers, getCardNum(card2)); // case file
			solver.addClause(clause2);

			int[] clause3 = new int[2];
			clause3[0] = getPairNum(getPlayerNum(suggester), getCardNum(card3));
			clause3[1] = getPairNum(numPlayers, getCardNum(card3)); // case file
			solver.addClause(clause3);
		}
	}

	// Accuse method
	public void accuse(String accuser, String card1, String card2, 
			String card3, boolean isCorrect)
	{
		if (isCorrect) {
			int[] clause1 = new int[1];
			clause1[0] = getPairNum(numPlayers, getCardNum(card1));
			solver.addClause(clause1);

			int[] clause2 = new int[1];
			clause2[0] = getPairNum(numPlayers, getCardNum(card2));
			solver.addClause(clause2);

			int[] clause3 = new int[1];
			clause3[0] = getPairNum(numPlayers, getCardNum(card3));
			solver.addClause(clause3);
		}
		else {
			int[] clause4 = new int[3];
			clause4[0] = 0 - getPairNum(numPlayers, getCardNum(card1));
			clause4[1] = 0 - getPairNum(numPlayers, getCardNum(card2));
			clause4[2] = 0 - getPairNum(numPlayers, getCardNum(card3));
			solver.addClause(clause4);
			
			// NEED TO CHECK: Should we assume perfect play?
			// The accuser does not have any of those 3 cards, assuming perfect play 
			int[] clause5 = new int[1];
			clause5[0] = 0 - getPairNum(getPlayerNum(accuser), getCardNum(card1));
			solver.addClause(clause5); 
			
			int[] clause6 = new int[1];
			clause6[0] = 0 - getPairNum(getPlayerNum(accuser), getCardNum(card2));
			solver.addClause(clause6);
			
			int[] clause7 = new int[1];
			clause7[0] = 0 - getPairNum(getPlayerNum(accuser), getCardNum(card3));
			solver.addClause(clause7);
		}
	}

	public int query(String player, String card) 
	{
		return solver.testLiteral(getPairNum(player, card));
	}

	public String queryString(int returnCode) 
	{
		if (returnCode == SATSolver.TRUE)
			return "Y";
		else if (returnCode == SATSolver.FALSE)
			return "n";
		else
			return "-";
	}

	public void printNotepad() 
	{
		PrintStream out = System.out;
		for (String player : players)
			out.print("\t" + player);
		out.println("\t" + caseFile);
		for (String card : cards) {
			out.print(card + "\t");
			for (String player : players) 
				out.print(queryString(query(player, card)) + "\t");
			out.println(queryString(query(caseFile, card)));
		}
	}

	public static void main(String[] args) 
	{
		ClueReasoner cr = new ClueReasoner();
		String[] myCards = {"wh", "li", "st"};
		cr.hand("sc", myCards);
//		cr.accuse("pe", "pe", "pi", "bi", false);
		cr.suggest("sc", "sc", "ro", "lo", "mu", "sc");
		cr.suggest("mu", "pe", "pi", "di", "pe", null);
		cr.suggest("wh", "mu", "re", "ba", "pe", null);
		cr.suggest("gr", "wh", "kn", "ba", "pl", null);
		cr.suggest("pe", "gr", "ca", "di", "wh", null);
		cr.suggest("pl", "wh", "wr", "st", "sc", "wh");
		cr.suggest("sc", "pl", "ro", "co", "mu", "pl");
		cr.suggest("mu", "pe", "ro", "ba", "wh", null);
		cr.suggest("wh", "mu", "ca", "st", "gr", null);
		cr.suggest("gr", "pe", "kn", "di", "pe", null);
		cr.suggest("pe", "mu", "pi", "di", "pl", null);
		cr.suggest("pl", "gr", "kn", "co", "wh", null);
		cr.suggest("sc", "pe", "kn", "lo", "mu", "lo");
		cr.suggest("mu", "pe", "kn", "di", "wh", null);
		cr.suggest("wh", "pe", "wr", "ha", "gr", null);
		cr.suggest("gr", "wh", "pi", "co", "pl", null);
		cr.suggest("pe", "sc", "pi", "ha", "mu", null);
		cr.suggest("pl", "pe", "pi", "ba", null, null);
		cr.suggest("sc", "wh", "pi", "ha", "pe", "ha");
		cr.suggest("wh", "pe", "pi", "ha", "pe", null);
		cr.suggest("pe", "pe", "pi", "ha", null, null);
		cr.suggest("sc", "gr", "pi", "st", "wh", "gr");
		cr.suggest("mu", "pe", "pi", "ba", "pl", null);
		cr.suggest("wh", "pe", "pi", "st", "sc", "st");
		cr.suggest("gr", "wh", "pi", "st", "sc", "wh");
		cr.suggest("pe", "wh", "pi", "st", "sc", "wh");
		cr.suggest("pl", "pe", "pi", "ki", "gr", null);
		cr.printNotepad();
		cr.accuse("sc", "pe", "pi", "bi", true);
//		cr.printNotepad();
	}           
}
