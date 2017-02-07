/*
 * Copyright (C) Bernhard Seybold. All rights reserved.
 *
 * This software is published under the terms of the LGPL Software License,
 * a copy of which has been included with this distribution in the LICENSE.txt
 * file.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *
 * $Id: Move.java,v 1.3 2003/01/04 16:09:21 BerniMan Exp $
 */

package chesspresso.move;

import chesspresso.*;


/**
 * Abstraction of a chess move.<br>
 *
 * This class provides support for two ways to encode moves:
 * <uol>
 *   <li>Based on <code>short</code>: optimized for speed and memory but cannot be
 *       used to print a SAN (short annotation, see PGN spec).
 *       Contains the following information: from square, to square, capturing,
 *       promotion piece.
 *   <li>Based on this class containing full information of the move such that
 *       a SAN (and LAN) of the move can be printed without further assistance.
 *       Contains all information of the short move plus information whether to
 *       to skip rank and file of the from square, check and mate information,
 *       the moving piece and whether or not is is a white move.<br>
 *       Internal representation is based on two shorts.
 * </ol>
 *
 * In order to create a full move out of a short move a position is needed.
 *
 * @author  Bernhard Seybold
 * @version $Revision: 1.3 $
 */
public class Move
{

    //======================================================================

    /**
     * Returns the moves in a normalized order such that the same set of moves
     * always yields the same order. Implementation is: short values ascending.
     */
    public static void normalizeOrder(short[] moves)
    {
        java.util.Arrays.sort(moves);
    }

    //======================================================================
    // move encoding (users of the class should abstract from implementation and
    // use accessors)
    //
    //      i n t        s h o r t
    //         2          1
    //       109876  5432109876543210
    //
    //               0000xxxxxxxxxxxx    specials
    //       cccmmm  0pppttttttffffff    regular move
    //       cccmmm  1pppttttttffffff    capturing move
    //       cccmmm  1110ttttttffffff    ep move
    //               1111xxxxxxxxxxxx    castles

    //       mmm     moving piece
    //       ccc     captured piece
    //       ppp     promotion piece   000 = specials, 111 = castles, 110 = ep, 101 - 001 promo pieces + 1 (5)
    //       tttttt  to sqi
    //       ffffff  from sqi
    //
    //  value 0 means NO_MOVE, allowing arrays of moves to be initialized with 0 (default)


    private final static int TYPE_MASK                = 0x00008000;
    private final static int REGULAR_MOVE             = 0x00000000;
    private final static int CAPTURING_MOVE           = 0x00008000;

    private final static int PROMO_MASK               = 0x00007000;
    private final static int CASTLE_MOVE              = 0x00007000;
    private final static int EP_MOVE                  = 0x00006000;
    private final static int PROMO_QUEEN              = 0x00005000;
    private final static int PROMO_ROOK               = 0x00004000;
    private final static int PROMO_BISHOP             = 0x00003000;
    private final static int PROMO_KNIGHT             = 0x00002000;
    private final static int NO_PROMO                 = 0x00001000;
    public  final static int SPECIAL_MOVE             = 0x00000000;  // allow defining of own specials
    public  final static int NUM_OF_SPECIAL_MOVES     = 0x00001000;

    private final static int FROM_SHIFT               =  0;
    private final static int TO_SHIFT                 =  6;
    private final static int PROMOTION_SHIFT          = 12;

    // precalculated castles moves
    public static final short
        WHITE_SHORT_CASTLE    = CASTLE_MOVE | Chess.E1 << FROM_SHIFT | Chess.G1 << TO_SHIFT,
        WHITE_LONG_CASTLE     = CASTLE_MOVE | Chess.E1 << FROM_SHIFT | Chess.C1 << TO_SHIFT,
        BLACK_SHORT_CASTLE    = CASTLE_MOVE | Chess.E8 << FROM_SHIFT | Chess.G8 << TO_SHIFT,
        BLACK_LONG_CASTLE     = CASTLE_MOVE | Chess.E8 << FROM_SHIFT | Chess.C8 << TO_SHIFT;

    /**
     * Represents "no move". Set to 0 to allow rapid initialization of arrays
     * to no moves (arrays are injitialized to 0 by Java).
     */
    public static final short NO_MOVE               = SPECIAL_MOVE;      // 0

    /**
     * Representing an illegal move.
     */
    public static final short ILLEGAL_MOVE          = SPECIAL_MOVE + 1;  // 1

    /**
     * The range <code>[OTHER_SPECIALS,OTHER_SPECIALS+NUM_OF_OTHER_SPECIALS[</code> is reserved
     * for clients of Move to define their own special moves. This can be used
     * to indicate special conditions when a move is expected. Moves of the
     * range above do not collide whith any other moves.
     */
    public static final short OTHER_SPECIALS        = SPECIAL_MOVE + 16; // first 16 special reserved for future use

    /**
     * Number of special moves which can be defined.
     */
    public static final short NUM_OF_OTHER_SPECIALS = NUM_OF_SPECIAL_MOVES - 16;

    private static final String
        SHORT_CASTLE_STRING = "O-O",                       // big letter o, not zero
        LONG_CASTLE_STRING  = "O-O-O";


    private static int[] s_promo = new int[Chess.MAX_PIECE + 1];

    static {
        for (int i=0; i<=Chess.MAX_PIECE; i++)
            s_promo[i] = NO_PROMO;
        s_promo[Chess.KNIGHT]   = PROMO_KNIGHT;
        s_promo[Chess.BISHOP]   = PROMO_BISHOP;
        s_promo[Chess.ROOK]     = PROMO_ROOK;
        s_promo[Chess.QUEEN]    = PROMO_QUEEN;
    }

    //======================================================================

    /**
     * Manufacture a regular move.
     *
     *@param fromSqi the from square
     *@param toSqi the to square
     *@param capturing whether or not it is a capturing move
     */
    public static short getRegularMove(int fromSqi, int toSqi, boolean capturing)
    {
        if (capturing) {
            return (short)(CAPTURING_MOVE | fromSqi << FROM_SHIFT | toSqi << TO_SHIFT | NO_PROMO);
        } else {
            return (short)(REGULAR_MOVE   | fromSqi << FROM_SHIFT | toSqi << TO_SHIFT | NO_PROMO);
        }
    }

    /**
     * Manufacture a pawn move.
     *
     *@param fromSqi the from square
     *@param toSqi the to square
     *@param capturing whether or not it is a capturing move
     *@param promotionPiece set to a piece if it is a promotion move, set to <code>No_PIECE</code> otherwise
     */
    public static short getPawnMove(int fromSqi, int toSqi, boolean capturing, int promotionPiece)
    {
        if (capturing) {
            return (short)(CAPTURING_MOVE | fromSqi << FROM_SHIFT | toSqi << TO_SHIFT | s_promo[promotionPiece]);
        } else {
            return (short)(REGULAR_MOVE   | fromSqi << FROM_SHIFT | toSqi << TO_SHIFT | s_promo[promotionPiece]);
        }
    }

    /**
     * Manufacture an en passant move.
     *
     *@param fromSqi the from square
     *@param toSqi the to square
     */
    public static short getEPMove(int fromSqi, int toSqi)
    {
        return (short)(CAPTURING_MOVE | fromSqi << FROM_SHIFT | toSqi << TO_SHIFT | EP_MOVE);
    }

    /**
     * Manufacture a short castle move.
     *
     *@param toPlay for which color
     */
    public static short getShortCastle(int toPlay)
    {
        return (toPlay == Chess.WHITE ? WHITE_SHORT_CASTLE : BLACK_SHORT_CASTLE);
    }

    /**
     * Manufacture a long castle move.
     *
     *@param toPlay for which color
     */
    public static short getLongCastle(int toPlay)
    {
        return (toPlay == Chess.WHITE ? WHITE_LONG_CASTLE : BLACK_LONG_CASTLE);
    }

    /*================================================================================*/

    public final static int getFromSqi       (short move) {return (move >> FROM_SHIFT) & 0x3F;}
    public final static int getToSqi         (short move) {return (move >> TO_SHIFT)   & 0x3F;}

    public final static boolean isCapturing  (short move) {return (move & TYPE_MASK) == CAPTURING_MOVE;}

    public final static boolean isPromotion  (short move) {int promo = move & PROMO_MASK; return promo == PROMO_QUEEN || promo == PROMO_ROOK || promo == PROMO_BISHOP || promo == PROMO_KNIGHT;}  // slow but safe
    public final static int getPromotionPiece(short move) {int promo = move & PROMO_MASK; for (int piece=0; piece<=Chess.MAX_PIECE; piece++) {if (s_promo[piece] == promo) return piece;} return Chess.NO_PIECE;}

    public final static boolean isEPMove     (short move) {return (move & PROMO_MASK) == EP_MOVE;}

    public static boolean isCastle           (short move) {return (move & PROMO_MASK) == CASTLE_MOVE;}
    public static boolean isShortCastle      (short move) {return  move == WHITE_SHORT_CASTLE | move == BLACK_SHORT_CASTLE;}
    public static boolean isLongCastle       (short move) {return  move == WHITE_LONG_CASTLE  | move == BLACK_LONG_CASTLE;}

    public static boolean isSpecial          (short move) {return (move & PROMO_MASK) == SPECIAL_MOVE;}
    public static boolean isValid            (short move) {return (move & PROMO_MASK) != SPECIAL_MOVE;}

    /*================================================================================*/

    public static String getBinaryString(short move)
    {
        StringBuffer sb = new StringBuffer();
        for (int i=15; i>=0; i--) {
            if ((move & (1 << i)) != 0) sb.append("1"); else sb.append("0");
        }
        return sb.toString();
    }

    /**
     * Returns a string representation of the move.
     *
     *@param the move
     *@return the string representation, e.g. e2xf4
     */
    public static String getString(short move)
    {
        if      (move == NO_MOVE)          return "<no move>";
        else if (move == ILLEGAL_MOVE)     return "<illegal move>";
        else if (isSpecial(move))          return "<special>";
        else if (isShortCastle(move))      return SHORT_CASTLE_STRING;
        else if (isLongCastle(move))       return LONG_CASTLE_STRING;
        else {
            StringBuffer sb = new StringBuffer();
            sb.append(Chess.sqiToStr(getFromSqi(move)));
            sb.append(isCapturing(move) ? 'x' : '-');
            sb.append(Chess.sqiToStr(getToSqi(move)));
            if (isPromotion(move)) sb.append(Chess.pieceToChar(getPromotionPiece(move)));
            return sb.toString();
        }
    }

    /*================================================================================*/

    private static final Move
        MOVE_ILLEGAL_MOVE = new Move (ILLEGAL_MOVE, Chess.NO_PIECE, Chess.NO_ROW, Chess.NO_COL, false, false, false);

    /**
     * Premanufactured illegal move, always returns the same instance.
     *
     *@return an illegal move
     */
    public static Move createIllegalMove()
    {
        return MOVE_ILLEGAL_MOVE;
    }

    /**
     * Convenience method to create a castle move.
     *
     *@param move a castling move to based upon, must be a castling move
     *@param isCheck whether the move gives a check
     *@param isMate whether the move sets mate
     *@param isWhite whether it is a white move
     *@return the castle move
     */
    public static Move createCastle(short move, boolean isCheck, boolean isMate, boolean whiteMove)
    {
        return new Move(move, Chess.KING, Chess.NO_COL, Chess.NO_ROW, isCheck, isMate, whiteMove);
    }

    /**
     * Convenience factory method to create a short castle move.
     *
     *@param toPlay the moving player
     *@param isCheck whether the move gives a check
     *@param isMate whether the move sets mate
     *@param isWhite whether it is a white move
     *@return the castle move
     */
    public static Move createShortCastle(int toPlay, boolean isCheck, boolean isMate, boolean whiteMove)
    {
        return new Move(getShortCastle(toPlay), Chess.KING, Chess.NO_COL, Chess.NO_ROW, isCheck, isMate, whiteMove);
    }

    /**
     * Convenience factory method to create a long castle move.
     *
     *@param toPlay the moving player
     *@param isCheck whether the move gives a check
     *@param isMate whether the move sets mate
     *@param isWhite whether it is a white move
     *@return the castle move
     */
    public static Move createLongCastle(int toPlay, boolean isCheck, boolean isMate, boolean whiteMove)
    {
        return new Move(getLongCastle(toPlay), Chess.KING, Chess.NO_COL, Chess.NO_ROW, isCheck, isMate, whiteMove);
    }

    /*================================================================================*/

    // encoding for additional information
    private static final int COL_FROM_MUL    = 0x00000010;
    private static final int COL_FROM_MASK   = 0x000000F0;
    private static final int ROW_FROM_MUL    = 0x00000100;
    private static final int ROW_FROM_MASK   = 0x00000F00;
    private static final int CHECK_MUL       = 0x00002000;
    private static final int CHECK_MASK      = 0x00002000;
    private static final int MATE_MUL        = 0x00004000;
    private static final int MATE_MASK       = 0x00004000;
    private static final int TOPLAY_MUL      = 0x00008000;
    private static final int TOPLAY_MASK     = 0x00008000;
    private static final int MOVING_MUL      = 0x00010000;
    private static final int MOVING_MASK     = 0x00070000;

    private short m_move;
    private int m_info;

    /*================================================================================*/

    /**
     * Creates a full move.
     *
     *@param move the short move
     *@param movingPiece the piece moving
     *@param colFrom file if should be taken for SAN, <code>NO_COL</code> otherwise
     *@param rowFrom rank if should be taken for SAN, <code>NO_ROW</code> otherwise
     *@param isCheck whether the move gives a check
     *@param isMate whether the move sets mate
     *@param isWhite whether it is a white move
     */
    public Move(short move, int movingPiece, int colFrom, int rowFrom, boolean isCheck, boolean isMate, boolean isWhiteMove)
    {
        m_move = move;
        m_info =
                 COL_FROM_MUL  * (colFrom - Chess.NO_COL)
               + ROW_FROM_MUL  * (rowFrom - Chess.NO_ROW)
               + (isCheck    ? CHECK_MUL   : 0)
               + (isMate     ? MATE_MUL    : 0)
               + (isWhiteMove  ? TOPLAY_MUL  : 0)
               + MOVING_MUL * movingPiece;
    }

    /*================================================================================*/

    public short getShortMoveDesc()     {return (short)m_move;}
    public int getPromo()               {return Move.getPromotionPiece(m_move);}
    public int getFromSqi()             {return Move.getFromSqi(m_move);}
    public int getToSqi()               {return Move.getToSqi(m_move);}
    public int getMovingPiece()         {return (m_info & MOVING_MASK) / MOVING_MUL;}
    public int getColFrom()             {return (m_info & COL_FROM_MASK) / COL_FROM_MUL + Chess.NO_COL;}
    public int getRowFrom()             {return (m_info & ROW_FROM_MASK) / ROW_FROM_MUL + Chess.NO_ROW;}
    public boolean isCapturing()        {return Move.isCapturing(m_move);}
    public boolean isPromotion()        {return Move.isPromotion(m_move);}
    public boolean isCheck()            {return (m_info & CHECK_MASK) != 0;}
    public boolean isMate()             {return (m_info & MATE_MASK) != 0;}
    public boolean isShortCastle()      {return Move.isShortCastle(m_move);}
    public boolean isLongCastle()       {return Move.isLongCastle(m_move);}
    public boolean isValid()            {return Move.isValid(m_move);}
    public boolean isWhiteMove()        {return (m_info & TOPLAY_MASK) != 0;}

    /*================================================================================*/

    /**
     * Equality test. Two move are equal if and only if all arguments match.
     *
     *@param obj the object to compare against
     *@return whether the two moves are equal
     */
    public boolean equals(Object obj)
    {
        if (obj instanceof Move) {
            Move move = (Move)obj;
            return m_move == move.m_move && m_info == move.m_info;
        } else {
            return false;
        }
    }

    /**
     * Returns the LAN (long annotation, see PGN spec) of the move, e.g. Ne2xf4+.
     *
     *@return the LAN representation
     */
    public String getLAN()
    {
        if (!isValid()) {
            return "<illegal move>";
        } else {
            StringBuffer sb = new StringBuffer();
            if (isShortCastle()) {
                sb.append(SHORT_CASTLE_STRING);
            } else if (isLongCastle()) {
                sb.append(LONG_CASTLE_STRING);
            } else {
                int piece = getMovingPiece();
                if (piece == Chess.NO_PIECE) System.out.println(m_move + " " + m_info + " " + Integer.toBinaryString(m_info));
                if (piece != Chess.PAWN) {
                    sb.append(Chess.pieceToChar(piece));
                }
                sb.append(Chess.sqiToStr(getFromSqi()));
                sb.append(isCapturing() ? "x" : "-");
                sb.append(Chess.sqiToStr(getToSqi()));
                if (isPromotion()) {
                    sb.append('=').append(Chess.pieceToChar(getPromo()));
                }
            }
            if      (isMate())  sb.append('#');
            else if (isCheck()) sb.append('+');
            return sb.toString();
        }
    }

    /** Modified by Laurent Bernabe : translate into a real SAN move (not a LAN variant) and made static.
     * Returns the SAN (short annotation, see PGN spec) of the move, e.g. Nxf4+.
     * @param move - short - the move we want the SAN.
     * @param positionBefore - Position - the position of the board, just before the move happens.
       Be careful, it should really be the position JUST BEFORE THE MOVE, NOT AFTER THE MOVE.
     *@return the SAN representation
     */
    public static String getSAN(short move, final Position positionBefore)
    {

      // test for move validity
      boolean isAValidMoveIntoPositionBefore = false;
      short [] positionBeforeMoves = positionBefore.getAllMoves();
      int positionBeforeMovesLength = positionBeforeMoves.length;
      for (int index = 0; index < positionBeforeMovesLength; index++){
        if (positionBeforeMoves[index] == move) {
          isAValidMoveIntoPositionBefore = true;
          break;
        }
      }
      if (!isAValidMoveIntoPositionBefore) return "<illegal move>";

      StringBuffer sb = new StringBuffer();
      if (Move.isShortCastle(move)) {
          sb.append(SHORT_CASTLE_STRING);
      } else if (Move.isLongCastle(move)) {
          sb.append(LONG_CASTLE_STRING);
      } else {
          int piece = getMovingPiece();
          if (piece == Chess.NO_PIECE) return "<invalid move : no piece is moving>";

          if (piece == Chess.PAWN){
            if (Move.isCapturing(move)) appendPawnCapturingSAN(move, sb, positionBefore);
            else appendPawnSimplePushingSAN(move, sb, positionBefore);
          }
          else appendStandardPieceMoveSAN(move, sb, positionBefore);
      }

      Position resultingPosition = clonePosition(positionBefore);
      resultingPosition.doMove(move);

      if      (resultingPosition.isMate())  sb.append('#');
      else if (resultingPosition.isCheck()) sb.append('+');
      return sb.toString();
    }

    ////////////// added by Laurent Bernabe //////////////////////////////

    private static void appendPawnCapturingSAN(short move, StringBuffer sb, final Position positionBefore){
      sb.append(Chess.sqiToStr(Move.getToSqi(move))); // adding 'to' square
      if (Move.isPromotion(move)) sb.append('=').append(Chess.pieceToChar(getPromo(move))); // adding promotion piece if needed
    }

    private static void appendPawnSimplePushingSAN(short move, StringBuffer sb, final Position positionBefore){
      sb.append(Chess.sqiToStr(Move.getFromSqi(move)).charAt(0)); // adding 'from' file
      sb.append('x');
      sb.append(Chess.sqiToStr(Move.getToSqi(move))); // adding 'to' square
      if (Move.isPromotion(move)) sb.append('=').append(Chess.pieceToChar(getPromo(move))); // adding promotion piece if needed
    }

    private static void appendStandardPieceMoveSAN(short move, StringBuffer sb, final Position positionBefore){
      final int movingPiece = getMovingPiece();
      final int moveToSqi = Move.getToSqi(move);
      sb.append(Chess.pieceToChar(movingPiece)); // adding piece symbol
      short [] positionBeforeMoves = positionBefore.getAllMoves(); // get all possible moves
      short [] samePieceTypeMoves = filter(positionBeforeMoves, new MovePredicate(){
        boolean isAcceptableMove(short scannedMove){
          int scannedMoveMovingPiece = positionBeforeMoves.getPiece(Move.getFromSqi(scannedMove));
          return movingPiece == scannedMoveMovingPiece;
        }
      });
      short [] leavingKingSafeMoves = filter(samePieceTypeMoves, new MovePredicate(){
        boolean isAcceptableMove(short scannedMove){
          try {
            Position clonedPosition = clonePosition(positionBefore);
            clonedPosition.doMove(scannedMove);
            return true;
          }
          catch (IllegalMoveException e){
            return false;
          }
        }
      });
      short [] sameToSquareIndexMoves = filter(leavingKingSafeMoves, new MovePredicate(){
        boolean isAcceptableMove(short scannedMove){
          int scannedMoveToSqi = Move.getToSqi(scannedMove);
          return scannedMoveToSqi == moveToSqi;
        }
      });

      int possibleMovesCount = sameToSquareIndexMoves.length;
      if (possibleMovesCount > 2){
        /*
        When we have more than two pieces of same king aiming for the same square so that
        all moves are legal, then we can't just try to find a common file/rank.
        */
        sb.append(Chess.sqiToStr(Move.getFromSqi(move))); //adding 'from' square in order to remove ambiguity
      }
      else if (possibleMovesCount == 2){
        String move0FromSqiStr = Chess.sqiToStr(Move.getFromSqi(sameToSquareIndexMoves[0]));
        String move1FromSqiStr = Chess.sqiToStr(Move.getFromSqi(sameToSquareIndexMoves[1]));

        boolean commonFile = move0FromSqiStr.charAt(0) == move1FromSqiStr.charAt(0);
        boolean commonRank = move0FromSqiStr.charAt(1) == move1FromSqiStr.charAt(1);

        if (commonFile){
          sb.append(move0FromSqiStr.charAt(0)); // adding common file in order to remove ambiguity
        }
        else if (commonRank){
          sb.append(move0FromSqiStr.charAt(1)); // adding common rank in order to remove ambiguity
        }
        else {
          sb.append("<common_coord_error>");
        }
      }

      if (Move.isCapturing(move)) sb.append('x'); // adding capture sign

      sb.append(Chess.sqiToStr(Move.getToSqi(move))) // adding target cell
    }

    private static interface MovePredicate {
      boolean isAcceptableMove(short move);
    }

    private static short[] filter(short[] inputArray, MovePredicate predicate){
      java.util.ArrayList<Short> dynamicResultsList = new java.util.ArrayList<Short>();
      int inputArrayLength = inputArray.length;
      for (int index = 0; index < inputArrayLength; index++){
        short currentMove = inputArray[index];
        if (predicate.isAcceptableMove(currentMove)) dynamicResultsList.add(currentMove);
      }

      short [] results = new short[dynamicResultsList.size()];
      for (int index = 0; index < dynamicResultsList.size(); index++){
        results[index] = dynamicResultsList.get(index);
      }

      return results;
    }

    private static Position clonePosition(Position originalPosition){
      return new Position(FEN.getFEN(originalPosition));
    }

    private static int getPromo(short move){
      return Move.getPromotionPiece(move);
    }

    ////////////// added by Laurent Bernabe - end of section //////////////////////////////

    /**
    Modified by Laurent Bernabe
    Using LAN instead of SAN, so no need for a Position instance
    */
    public String toString() {return getLAN();}

}
