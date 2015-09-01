;;;
;
; Author: Lee Burnette/El'endia Starman
; Date: 2013-03-30
;
; Name: ArbyChess
; Function: A chess engine that allows arbitrary boards and piece moves
;
;;;

; ; graphics selector goes here ; ;
.Graphics

Graphics 800, 600

; types ;
.types

Type board
	Field cells.celloplayer ;Ha! Pun!
	Field ramifying ;oversimply, whether a bishop can go the same distance in the same direction and end up in one of two places
End Type

Type cell
	Field x, y ;for drawing image
	Field img
	Field horse.cell[4] ;Ha! Another pun!
	Field p.piece
End Type

Type piece
	Field x, y ;for drawing image
	Field img
	Field c.cell ;which cell the piece is on
	Field m.movelist ;available moves
	Field mobile
	Field alive
	Field name$
	Field players.player[10] ;allow the possibility of multiple players controlling the same piece
	;Field ...
End Type

Type move
	Field dx, dy
	Field vector ;1-4
	Field options[10] ;flags
	Field counter[10] ;counting number of moves, etc.
	Field nex.movelist ;available next moves
End Type

Type movelist
	Field m.move
	Field bef.movelist, nex.movelist ;doubly-linked list
End Type

Type celloplayer
	Field c.cell
	Field bef.celloplayer, nex.celloplayer
End Type

Type mouse
	Field x, y
	Field c.cell
	Field selector ;if a cell has been selected
End Type

Type game
	Field b.board
	Field m.mouse
	;Field c.cards
End Type

Type player
	Field counter[10] ;score, time, etc.
	Field name$
End Type

; functions ;
.functions

;game functions
Function initialize.game()

End Function

Function run(g.game)

End Function

Function finish(g.game)

End Function

;board functions
Function import(b.board, path$="")

End Function

Function export(b.board, path$="")

End Function

Function display(b.board)

End Function

Function update(b.board) ;needed?
End Function

Function checkWin(b.board)

End Function

Function undo(b.board, oldb.board) ;implement?
End Function

;piece functions
Function choices.celloplayer(p.piece)

End Function

Function movePiece(p.piece, m.move)

End Function

Function possibleCells.celloplayer(c.cell, m.move)

End Function

;mouse functions
Function checkInput(m.mouse)

End Function