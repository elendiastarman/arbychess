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
SeedRnd(MilliSecs())
initialize()
WaitKey
End

; types ;
.types

Type board
	Field cells.celloplayer ;Ha! Pun!
	Field prevMoves.historyList
	Field specials.specialsList
	Field current.player
	Field ramifying ;oversimply, whether up then over ends in a different place than over then up
End Type

Type cell
	Field x, y ;for drawing image
	Field px, py ;for positioning piece image
	Field img, alt ;usual image, alt image
	Field dullhorse.cell[4] ;Ha! Another pun!  ;1=north, 2=east, 3=south, 4=west
	Field dullhorseIds[4]
	Field p.piece
	Field id
	Field visited
End Type

Type piece
	Field x, y ;for drawing image
	Field img
	Field c.cell ;which cell the piece is on
	Field m.movelist ;available moves
	Field mobile
	Field alive
	Field name$
	Field id
	Field players.playerlist ;allow the possibility of multiple players controlling the same piece
	;Field ...
End Type

Type move
	Field dx, dy
	;Field vector ;values of 1-9 ; "momentum" direction
	Field options[10] ;flags
	Field counter[10] ;counting number of moves, etc.
	Field nextMoves.movelist ;available subsequent moves
	Field nexIds[1000];
	Field chainMoves.movelist ;available chained moves
	Field chainIds[1000]
	Field id
End Type

Type special
	Field name$
	Field params[1000]
End Type

Type movelist
	Field m.move
	Field bef.movelist, nex.movelist ;doubly-linked list
End Type

Type celloplayer
	Field c.cell
	Field bef.celloplayer, nex.celloplayer ;doubly-linked list
End Type

Type cellMoveList
	Field c.cell
	Field mv.move
	Field srcmv.move
	Field bef.cellMoveList, nex.cellMoveList
End Type

Type playerlist
	Field p.player
	Field bef.playerlist, nex.playerlist
End Type

Type historyList
	Field m.move ;move used
	Field p.piece ;piece moved
	Field srcCell.cell, destCell.cell ;cell moved from and to
	Field capId, capX, capY, capCell.cell, capP.piece ;capId = id of piece that was captured; 0 if no piece was captured
	Field prevmvs.moveList, capprevmvs.moveList
	Field playa.player
	Field other.historyList ;for moves like castling, where there are two (or more) pieces to put back
	Field bef.historyList, nex.historyList
End Type

Type specialsList
	Field s.special
	Field bef.specialsList, nex.specialsList
End Type

Type mouse
	Field x, y
	Field c.cell
	Field selector ;if a cell has been selected (needed?)
	Field hit
	Field down
End Type

Type game
	Field b.board
	Field m.mouse
	;Field c.cards
End Type

Type player
	Field id
	Field counter[10] ;score, time, etc.
	Field name$
End Type

; functions ;
.functions

;game functions
.gameFuncs
Function initialize.game()

	g.game = New game
	b.board = New board
	m.mouse = New mouse
	
	g\b = b
	g\m = m
	
	;Stop
	
	;success = import(b, "hex.txt") ;for now, the simple game called Hex
	success = import(b, "chess.txt") ;gonna try it with chess
	
	If success
		g\b\prevMoves = New historyList
		run(g)
	Else
		DebugLog "Import failed."
		finish(g.game)
	EndIf

End Function

Function run(g.game)
	DebugLog "Ramifying? "+g\b\ramifying

	ClsColor 100, 100, 100
	Cls
	
	SetBuffer(BackBuffer())
	
	selected.cell = Null
	allowed.cellMoveList = Null
	g\b\current = First player
	nextPlayer.player = Null
	
	;select player with lowest id
	minid = g\b\current\id
	For p.player = Each player
		If p\id < minid
			minid = p\id
			g\b\current = p
		EndIf
	Next
	
	DebugLog "Current player: "+g\b\current\name+", id: "+g\b\current\id

	While Not KeyHit(1)
	
		If checkInput(g\m)
		
			If g\m\hit = 2 ;undo
				undo(g\b)
				g\m\selector = 0
				clearCellMoveList(allowed)
				clearCellAlts()
			EndIf
		
			If g\m\selector = 1 And g\m\hit = 1 ;first selection
			
				checkAllSpecials(g\b, 0)
			
				If g\m\c <> Null
					If g\m\c\p <> Null
						If playerInList(g\m\c\p\players, g\b\current)
						
							selected = g\m\c
							
							g\m\c\alt = LoadImage("cellSelected.png")
							If g\m\c\alt <> 0
								MaskImage g\m\c\alt, 1, 1, 1
							EndIf
							
							allowed = possibleCells(g\m\c, g\m\c\p\m, g\b\ramifying, g\b\current)
							
							While allowed <> Null
								If allowed\c <> Null
									
									allowed\c\alt = LoadImage("cellAllow.png")
									If allowed\c\alt <> 0
										MaskImage allowed\c\alt, 1, 1, 1
									EndIf
									
									If allowed\c\p <> Null
										If allowed\c\p\players <> Null
											If playerInList(allowed\c\p\players, g\b\current) = 1 ;can't take your own piece
												allowed\c\alt = 0
											EndIf
										EndIf
									EndIf
									
								EndIf
							
								If allowed\nex <> Null ;need to stop on the last one so the list can still be accessed when actually moving
									allowed = allowed\nex
								Else
									Exit
								EndIf
							Wend
						
						Else
							g\m\selector = 0 ;reset if selected doesn't belong to current player
						EndIf
						
					Else
						g\m\selector = 0 ;reset if empty square selected
					EndIf
				EndIf
				
			ElseIf g\m\selector = 2 Or g\m\selector = 3

				If g\m\selector = 2 ;second selection
					If g\m\c <> Null
						cml.cellMoveList = cellInList_cml(allowed, g\m\c)
						If cml <> Null
							hist.historyList = g\b\prevMoves
							hist\nex = New historyList
							hist\nex\bef = hist
							hist = hist\nex
							
							movePiece(selected, g\m\c, hist)
							
							;Stop
							
							If hist <> Null
								hist\m = cml\srcmv
								hist\prevmvs = g\m\c\p\m
								hist\playa = g\b\current
								;DebugLog "bing! "+hist\m\id+" "+hist\srcCell\id+":"+hist\destCell\id
							EndIf
							
							g\b\prevMoves = g\b\prevMoves\nex
							
							g\m\c\p\m = cml\srcmv\nextMoves
							
							checkAllSpecials(g\b, 0)
							
							;If KeyHit(57)
							;	s = 1-s
							;EndIf
							;If s = 1
							;	Stop
							;EndIf
							
							;garbage-collect -allowed-
							While allowed\bef <> Null
								allowed = allowed\bef
							Wend
							While allowed <> Null
								If allowed\nex = Null
									Delete allowed
								Else
									allowed = allowed\nex
									Delete allowed\bef
								EndIf
							Wend
							Delete cml
														
							;pick next player
							mindiff = 2^30-2
							pickFirst = 1
							
							For p.player = Each player
								If p <> g\b\current
									If p\id - g\b\current\id > 0
										pickFirst = 0
										If p\id - g\b\current\id < mindiff
											mindiff = p\id-g\b\current\id
											nextPlayer = p
										EndIf
									EndIf
								EndIf
							Next
							
							If pickFirst = 1 ;repick first player if previous player was last
								minid = g\b\current\id
								For p.player = Each player
									If p\id < minid
										minid = p\id
										nextPlayer = p
									EndIf
								Next
							EndIf
							
							g\b\current = nextPlayer
							DebugLog "Current player: "+g\b\current\name+", id: "+g\b\current\id
						EndIf
					EndIf
				EndIf
				
				selected = Null
				clearCellAlts()
				g\m\selector = 0

			EndIf
		
		EndIf
		
		Cls
		display(g\b)
		Flip
		
	Wend
	
	display(g\b)
	
	WaitKey

	finish(g)

End Function

Function finish(g.game)

	DebugLog "Exiting..."

	For pc.piece = Each piece
		Delete pc
	Next
	DebugLog "Pieces deleted."
	
	For ce.cell = Each cell
		Delete ce
	Next
	DebugLog "Cells deleted."
	
	For mo.move = Each move
		Delete mo
	Next
	DebugLog "Moves deleted."
	
	For ml.movelist = Each movelist
		Delete ml
	Next
	DebugLog "Movelist cleared."
	
	For cl.celloplayer = Each celloplayer
		Delete cl
	Next
	DebugLog "Celloplayer cleared."
	
	For mu.mouse = Each mouse
		Delete mu
	Next
	DebugLog "Mouse deleted."
	
	For bo.board = Each board
		Delete bo
	Next
	DebugLog "Board deleted."
	
	For py.player = Each player
		Delete py
	Next
	DebugLog "Players deleted."
	
	For ga.game = Each game
		Delete ga
	Next
	DebugLog "Game ended."
	
	End

End Function

;board functions
.boardFuncs
Function import(b.board, path$="")

	;;
	;assumed file format:
	;  "players"
	;  id | name$ | [counter values]
	;  ""
	;  "moves"
	;  id,dx,dy | move,capture[,non-jumping] | [counter values] | [chained moves' ids] | next moves' ids
	;  ""
	;  "pieces"
	;  id | imgx,imgy,imgsrc | mobile,name$ | moves' ids
	;  ""
	;  "specials"
	;  name$ | [params]
	;  ""
	;  "cells"
	;  id | imgx,imgy,imgsrc | northId,eastId,southId,westId | [piece id, piece imgx, piece imgy] | [player(s) id(s)]
	;;
	

	If path = "" ;file chooser
		;put navigation here
	EndIf

	file = ReadFile(path)
	
	segment$ = ReadLine$(file) ;another pun!
	While segment = "" Or Mid$(segment, 1, 1)=";" ;blank line or comment (starts with ;)
		segment = ReadLine(file)
	Wend
	
	;WaitKey
	
	;players
	;segment = ReadLine$(file)
	If segment = "players"
		DebugLog "players"
		segment = ReadLine$(file)
		While segment <> "" And Eof(file)=0
		
			;parsing - id | name$ | [counter values]
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				pos = 0 ;string read position
				pl.player = New player
				
				pl\id = Int( Mid$( segment, pos+1, Instr(segment, "|", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				pl\name = Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 )
					
				pos = Instr(segment, "|", pos+1)
				
				If pos+1 <= Len(segment) ;if there is a counter value

					If Instr(segment, ",", pos+1) = 0 ;if there's only one value
						pl\counter[1] = Int( Mid$( segment, pos+1, Len(segment)-pos ) )
						pl\counter[0] = 1
					Else ;comma separated list
						segment = segment+",," ;bit of a trick
						While Instr(segment, ",", pos+1) < Len(segment)
							tempC = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
							pos = Instr(segment, ",", pos+1)
							
							pl\counter[ pl\counter[0] ] = tempC
							pl\counter[0] = pl\counter[0]+1
							
							If pl\counter[0] > 9
								Exit
							EndIf
						Wend
					EndIf
				
				EndIf
			
			EndIf
			
			segment = ReadLine$(file)
		Wend
	
	Else
		Return 0
	EndIf

	
	;moves
	segment = ReadLine$(file)
	If segment = "moves"
		DebugLog "moves"
		segment = ReadLine$(file)
		While segment <> "" And Eof(file)=0
		
			;parsing - id,dx,dy | move,capture[,non-jumping] | [counter values] | [chained moves' ids] | next moves' ids
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				segment = segment+",," ;bit of a trick
				pos = 0 ;string read position
				m.move = New move
				
				m\id = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				
				m\dx = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				m\dy = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				m\options[1] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) ) ;move
				pos = Instr(segment, ",", pos+1)
				m\options[2] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) ) ;capture
				If Instr(segment, ",", pos+1) < Instr(segment, "|", pos+1) ;meaning there's a third number
					pos = Instr(segment, ",", pos+1)
					m\options[3] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) ) ;non-jumping
				EndIf
				
				pos = Instr(segment, "|", pos+1)
				If Instr(segment, "|", pos+1) - pos > 1 ;if there are counter values

					If Instr(segment, ",", pos+1) > Instr(segment, "|", pos+1) ;if there's only one value
						m\counter[1] = Int( Mid$( segment, pos+1, Instr(segment, "|", pos+1)-pos-1 ) )
						m\counter[0] = 1
					Else ;comma separated list
						While Instr(segment, ",", pos+1) < Instr(segment, "|", pos+1)
							tempC = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
							pos = Instr(segment, ",", pos+1)
							
							m\counter[ m\counter[0]+1 ] = tempC
							m\counter[0] = m\counter[0]+1
							
							If m\counter[0] > 9
								Exit
							EndIf
						Wend
					EndIf

				EndIf
				
				pos = Instr(segment, "|", pos+1)
				If Instr(segment, "|", pos+1) - pos > 1 ;if there are chained move ids

					If Instr(segment, ",", pos+1) > Instr(segment, "|", pos+1) ;if there's only one value
						m\chainIds[1] = Int( Mid$( segment, pos+1, Instr(segment, "|", pos+1)-pos-1 ) )
						m\chainIds[0] = 1
					Else ;comma separated list
						While Instr(segment, ",", pos+1) < Instr(segment, "|", pos+1)
							tempC = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
							pos = Instr(segment, ",", pos+1)
							
							m\chainIds[ m\chainIds[0]+1 ] = tempC
							m\chainIds[0] = m\chainIds[0]+1
							
							If m\chainIds[0] > 999
								Exit
							EndIf
						Wend
					EndIf

				EndIf
				
				pos = Instr(segment, "|", pos+1)
				
				While Instr(segment, ",", pos+1) < Len(segment)
					tempId = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					pos = Instr(segment, ",", pos+1)
					
					m\nexIds[ m\nexIds[0]+1 ] = tempId
					m\nexIds[0] = m\nexIds[0]+1
					
					If m\nexIds[0] > 999
						Exit
					EndIf
				Wend

			EndIf
			
			segment = ReadLine$(file)
		Wend
	Else
		Return 0
	EndIf
	
	;pieces
	segment = ReadLine$(file)
	If segment = "pieces"
		DebugLog "pieces"
		segment = ReadLine$(file)
		While segment <> "" And Eof(file)=0
		
			;parsing - id | imgx,imgy,imgsrc | mobile,name$ | moves' ids
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				segment = segment+",," ;bit of a trick
				pos = 0 ;string read position
				p.piece = New piece
				
				p\id = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				p\x = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				p\y = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				p\img = LoadImage( Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 ) )
				MaskImage p\img, 181, 230, 29
				MidHandle p\img
				
				pos = Instr(segment, "|", pos+1)
				
				;DrawImage p\img, p\x+GraphicsWidth()/2+50*Rand(0,2), p\y+GraphicsHeight()/2
				
				p\mobile = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				p\name = Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 )
				
				pos = Instr(segment, "|", pos+1)
				
				ml1.movelist = New movelist
				While Instr(segment, ",", pos+1) < Len(segment)
					tempId = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					pos = Instr(segment, ",", pos+1)
					
					ml2.movelist = New movelist
					ml2\m = fetchMoveById.move(tempId)
					ml2\bef = ml1
					ml1\nex = ml2
					ml1 = ml1\nex
				Wend
				
				While ml1\bef\bef <> Null
					ml1 = ml1\bef
				Wend
				p\m = ml1
				
			EndIf
			
			segment = ReadLine$(file)
		Wend
	Else
		Return 0
	EndIf
	
	;specials
	segment = ReadLine$(file)
	If segment = "specials"
		DebugLog "specials"
		b\specials = New specialsList
		segment = ReadLine$(file)
		While segment <> "" And Eof(file)=0
		
			;parsing - name$ | [counter values]
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				pos = 0 ;string read position
				spec.special = New special
				
				spec\name = Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 )
					
				pos = Instr(segment, "|", pos+1)
				
				If pos+1 <= Len(segment) ;if there is a counter value

					If Instr(segment, ",", pos+1) = 0 ;if there's only one value
						spec\params[1] = Int( Mid$( segment, pos+1, Len(segment)-pos ) )
						spec\params[0] = 1
					Else ;comma separated list
						segment = segment+",," ;bit of a trick
						While Instr(segment, ",", pos+1) < Len(segment)
							tempC = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
							pos = Instr(segment, ",", pos+1)
							
							spec\params[ spec\params[0] ] = tempC
							spec\params[0] = spec\params[0]+1
							
							If spec\params[0] > 999
								Exit
							EndIf
						Wend
					EndIf
				
				EndIf
				
				If b\specials\s <> Null
					b\specials\nex = New specialsList
					b\specials\nex\bef = b\specials
					b\specials = b\specials\nex
				EndIf
				
				b\specials\s = spec
			
			EndIf
			
			segment = ReadLine$(file)
		Wend
		
		If b\specials\s = Null
			Delete b\specials
		EndIf
	
	Else
		Return 0
	EndIf
	
	;cells
	segment = ReadLine$(file)
	If segment = "cells"
		DebugLog "cells"
		cl1.celloplayer = New celloplayer
			
		segment = ReadLine$(file)
		While segment <> ""
		
			;parsing - id | imgx,imgy,imgsrc | northId,eastId,southId,westId | [piece id, piece imgx, piece imgy] | [player(s) id(s)]
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				pos = 0 ;string read position
				c.cell = New cell
				
				c\id = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				c\x = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\y = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\img = LoadImage( Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 ) )
				MaskImage c\img, 181, 230, 29
				;MidHandle p\img
				
				pos = Instr(segment, "|", pos+1)
				
				c\dullhorseIds[1] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\dullhorseIds[2] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\dullhorseIds[3] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\dullhorseIds[4] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				If Instr(segment, "|", pos+1) - pos > 1 ;if there is a piece
					tempId = Int( Mid$( segment, pos+1, Instr(segment, "|", pos+1)-pos-1 ) )
					c\p = copyPiece( fetchPieceById(tempId) )
					c\p\c = c
					c\p\players = New playerlist
					;DebugLog c\p\c\id
					
					pos = Instr(segment, ",", pos+1)
					c\px = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					pos = Instr(segment, ",", pos+1)
					c\py = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					
					pos = Instr(segment, "|", pos+1)
					
					If pos+1 <= Len(segment) ;if there are players owning this piece
					
						If Instr(segment, ",", pos+1) = 0 ;if there's only one value
							tempId = Int( Mid$( segment, pos+1, Len(segment)-pos ) )
							
							;appendPlayer(c\p\players, fetchPlayerById(tempId))
							c\p\players\p = fetchPlayerById(tempId)
						Else ;comma separated list
							segment = segment+",," ;bit of a trick

							While Instr(segment, ",", pos+1) < Len(segment)
								tempId = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
								pos = Instr(segment, ",", pos+1)
								
								If c\p\players = Null
									c\p\players\p = fetchPlayerById(tempId)
								Else
									appendPlayer(c\p\players, fetchPlayerById(tempId))
								EndIf
							Wend
						EndIf
						
						DebugLog c\p\players\p\name
					
					EndIf
				
				EndIf
				
				cl2.celloplayer = New celloplayer
				cl2\c = c
				cl2\bef = cl1
				cl1\nex = cl2
				cl1 = cl1\nex
			
			EndIf
			
			segment = ReadLine$(file)
		Wend
	Else
		Return 0
	EndIf
	
	While cl1\bef\bef <> Null
		cl1 = cl1\bef
	Wend
	b\cells = cl1
	
	link(b.board) ;turn all ids into pointers
	b\ramifying = isRamifying(b\cells)
	
	Return 1

End Function

Function export(b.board, path$="")

End Function

Function link(b.board) ;replaces id identifiers with pointer references

	;move -> move
	For m.move = Each move
		ml1.movelist = New movelist
		
		For i = 1 To m\nexIds[0]
		
			ml2.movelist = New movelist
			ml2\m = fetchMoveById( m\nexIds[i] )
			ml2\bef = ml1
			ml1\nex = ml2
			ml1 = ml1\nex
		
		Next
		
		While ml1\bef\bef <> Null
			ml1 = ml1\bef
		Wend
		
		m\nextMoves = ml1
		
		If m\chainIds[0] > 0
			ml3.movelist = New movelist
			
			For j = 1 To m\chainIds[0]
			
				ml4.movelist = New movelist
				ml4\m = fetchMoveById( m\chainIds[j] )
				ml4\bef = ml3
				ml3\nex = ml4
				ml3 = ml3\nex
			
			Next
			
			While ml3\bef\bef <> Null
				ml3 = ml3\bef
			Wend
			
			m\chainMoves = ml3
		EndIf
		
	Next
	
	;cell -> cell
	For c.cell = Each cell
		For i = 1 To 4
		
			c\dullhorse[i] = fetchCellById( c\dullhorseIds[i] )
			;DebugLog c\dullhorse[i]\id
		
		Next
	Next

End Function

Function isRamifying(cells.celloplayer)

	;"ramifying" refers to whether order matters
	;That is, if up then over ends in a different place than over then up,
	;then the board is ramifying.
	
	While cells <> Null
	
		For dy = -1 To 1 Step 2
			For dx = -1 To 1 Step 2
			
				dests.celloplayer = destinations(cells\c, dx, dy, 1)
				If dests\nex <> Null ;there are two destinations
					Return 1
				EndIf
				
			Next
		Next
		
		cells = cells\nex
	
	Wend
	
	Return 0

End Function

Function display(b.board)

	cells.celloplayer = b\cells
	While cells <> Null
		DrawImage cells\c\img, cells\c\x, cells\c\y
		
		If cells\c\p <> Null ;if the cell has a piece on it
			DrawImage cells\c\p\img, cells\c\x + cells\c\px, cells\c\y + cells\c\py
		EndIf
		
		;DebugLog cells\c\alt
		If cells\c\alt <> 0 ;if the cell is selected or move-to-able
			DrawImage cells\c\alt, cells\c\x, cells\c\y
		EndIf
		
		cells = cells\nex
	Wend
	
	Text 20,20,b\current\name

End Function

Function checkWin(b.board)

End Function

Function undo(b.board)

	hist.historyList = b\prevMoves

	If hist\bef <> Null
		
		;if any, take care of the other moves too (like the rook move of castling)
		While hist\other <> Null

			hist\other\srcCell\px = hist\other\destCell\px
			hist\other\srcCell\py = hist\other\destCell\py
			hist\other\srcCell\p  = hist\other\destCell\p
			hist\other\destCell\p = Null
			
			If hist\other\capId > 0 ;supposing that other pieces were captured as well and need to be restored...
				hist\other\capCell\p = hist\other\capP
				hist\other\capCell\px = hist\other\capx
				hist\other\capCell\py = hist\other\capy
				hist\other\capCell\p\m = hist\other\capprevmvs
			EndIf
			
			hist\other = hist\other\nex
		Wend
		
		;move piece back
		hist\srcCell\px = hist\destCell\px
		hist\srcCell\py = hist\destCell\py
		hist\srcCell\p  = hist\destCell\p
		hist\srcCell\p\m = hist\prevmvs
		hist\destCell\p = Null
		
		;restore captured piece, if any
		If hist\capId > 0
			hist\capCell\p = hist\capP
			hist\capCell\px = hist\capx
			hist\capCell\py = hist\capy
			hist\capCell\p\m = hist\capprevmvs
		EndIf
		
		;rewind turn to previous player
		b\current = hist\playa
		
		;move current history back
		hist = hist\bef
		b\prevMoves = b\prevMoves\bef
		
		checkAllSpecials(b, 1)
		
	EndIf
	
End Function

;piece/cell/move functions
.pieceFuncs

Function movePiece(start.cell, dest.cell, hist.historyList)

	If dest\p <> Null
		;dest\p\alive = 0
		hist\capId = dest\p\id
		hist\capP = dest\p
		hist\capx = dest\px
		hist\capy = dest\py
		hist\capprevmvs = dest\p\m
		hist\capCell = dest
	EndIf
	
	hist\srcCell = start
	hist\destCell = dest
	hist\p = start\p
	
	dest\px = start\px
	dest\py = start\py
	dest\p = start\p
	start\p = Null

End Function

Function possibleCells.cellMoveList(c.cell, moves.movelist, ramifying, playa.player)

	For c2.cell = Each cell ;force precondition: cells have not been visited before
		c2\visited = 0
	Next
	
	c\visited = 1

	;for each move, destination cells
	;for each destination cell, recurse
	
	final.cellMoveList = New cellMoveList
	final\c = c
	final\mv = moves\m
	moves = moves\nex
	
	While moves <> Null
		final\nex = New cellMoveList
		final\nex\c = c
		final\nex\mv = moves\m
		final\nex\bef = final
		final = final\nex
	
		moves = moves\nex
	Wend
	
	fin.cellMoveList = final
	While fin\bef <> Null
		fin = fin\bef
	Wend

	dests1.celloplayer = Null
	dests2.celloplayer = Null
	;dests3.celloplayer = Null
	
	While fin <> Null
	
		If fin\mv <> Null
		
			;find destinations with current move
			dests1 = destinations(fin\c, fin\mv\dx, fin\mv\dy, ramifying, fin\mv\options[3]) ;get destination(s)
			
			While dests1 <> Null
				If dests1\c <> Null
					If dests1\c\visited = 0
						allow = 0
					
						op1 = fin\mv\options[1] ;0 = can't move, 1 = move once, 2 = chain, 3 = branch
						op2 = fin\mv\options[2] ;same, but with capture
						
						If op1 >= 1 And dests1\c\p = Null
							allow = op1
						ElseIf op2 >= 1 And dests1\c\p <> Null
							If playerInList(dests1\c\p\players, playa) = 0
								allow = op2
							EndIf
						EndIf
						
						If allow >= 1
							final\nex = New cellMoveList
							final\nex\c = dests1\c
							dests1\c\visited = 1
							
							If allow = 1
								final\nex\mv = Null
								final\nex\srcmv = fin\mv
							ElseIf allow = 2
								final\nex\mv = fin\mv
								final\nex\srcmv = fin\mv
							EndIf
							
							final\nex\bef = final
							final = final\nex
						EndIf
						
					EndIf
				EndIf
				
				;garbage-collect -dests1-
				If dests1\nex = Null
					Delete dests1
				Else
					dests1 = dests1\nex
					Delete dests1\bef
				EndIf
			Wend
			
			;find destinations with chain moves
			While fin\mv\chainMoves <> Null
	
				dests2 = destinations(fin\c, fin\mv\chainMoves\m\dx, fin\mv\chainMoves\m\dy, ramifying, fin\mv\chainMoves\m\options[3]) ;get destination(s)
				
				While dests2 <> Null ;loop through destinations
					If dests2\c <> Null
						If dests2\c\visited = 0 ;if I haven't been there before
							allow = 0
						
							op1 = fin\mv\chainMoves\m\options[1] ;0 = can't move, 1 = move once, 2 = chain, 3 = branch
							op2 = fin\mv\chainMoves\m\options[2] ;same, but with capture
							
							If op1 > 0 And dests2\c\p = Null
								allow = op1
							ElseIf op2 > 0 And dests2\c\p <> Null
								If playerInList(dests2\c\p\players, playa)=0
									allow = op1
								EndIf
							EndIf
							
							If allow >= 1
								final\nex = New cellMoveList
								final\nex\c = dests2\c
								dests2\c\visited = 1
								
								If allow = 1
									final\nex\mv = Null
									final\nex\srcmv = fin\mv\chainMoves\m
								ElseIf allow = 2
									final\nex\mv = fin\mv\chainMoves\m
									final\nex\srcmv = fin\mv\chainMoves\m
								EndIf

								final\nex\bef = final
								final = final\nex
							EndIf
							
						EndIf
					EndIf
					
					;garbage-collect -dests2-
					If dests2\nex = Null
						Delete dests2
					Else
						dests2 = dests2\nex
						Delete dests2\bef
					EndIf
				Wend
				
				fin\mv\chainMoves = fin\mv\chainMoves\nex
				
			Wend
		EndIf
	
		fin = fin\nex
	
	Wend
	
	While final\bef <> Null ;rewind cell list	
		final = final\bef
	Wend
	
	While final\nex <> Null And final\c = c
		final = final\nex
		Delete final\bef
		
		If final\nex = Null
			If final\c = c
				Delete final
				Return Null
			Else
				Return final
			EndIf
		EndIf
	Wend

	Return final

End Function

Function destinations.celloplayer(c.cell, dx, dy, checkAlt=0, nonjumping=0)

	;rationale for choosing to check only a zig-zag pattern:
	
	;- Checking every route that consists of |dx| steps in the
	;x direction and |dy| steps in the y direction scales poorly.
	;The formula is Binom(|dx|+|dy|, |dx|). Max value is reached
	;when |dx| = |dy|, and when |dx|=|dy|=9, Binom(18,9) > 2^31-1,
	;which is the largest value that a Blitz int can hold.
	
	;- Another possibility is checking all semi-zig-zag patterns,
	;that is, patterns where no more than 2 steps are taken in
	;either the x or y directions before changing direction.
	;The formula for that is 2^(min(|dx|,|dy|)), which scales
	;better than checking every route, but still poorly. It also
	;has an upper bound like the previous method.
	
	;- The two remaining sensible and simple options are to take
	;the sides of a rectangle (do all of dx first, then all of dy)
	;or to zig-zag (alternate dx and dy). When considering straight
	;diagonal moves (|dx| ~ |dy|), zig-zagging is closer to the
	;spirit of a diagonal move. Thus, the same spirit shall be
	;applied to non-purely diagonal moves where dx and dy do not
	;have approximately the same magnitude.
	
	;Hence, in the case where order matters, x and y steps shall
	;be chosen so as to remain as close as possible to the straight
	;line connecting (0,0) to (dx,dy).
	
	;tl;dr - diagonal moves will be checked as close to diagonal as possible.
	;At most 2 cells will be returned from this function.
	
	x = 0
	y = 0
	sx = Sgn(dx)
	sy = Sgn(dy)
	
	dests.celloplayer = New celloplayer
	curr.cell = c
	
	; x| y| cell neighbor
	;  |-1| 1 (north)
	;-1|  | 4 (west)
	;  | 1| 3 (south)
	; 1|  | 2 (east)
	
	While x <> dx Or y <> dy
	
		If Abs( (x+sx)*dy ) < Abs( (y+sy)*dx ) Or (Abs( (x+sx)*dy ) = Abs( (y+sy)*dx ) And checkAlt=1) Or dy=0
		
			If curr\dullhorse[3-sx] = Null
				Exit
			Else
				curr = curr\dullhorse[3-sx]
				
				If nonjumping = 1
					If curr\p <> Null
						Exit
					EndIf
				EndIf
				
				x = x+sx
			EndIf
			
		ElseIf Abs( (x+sx)*dy ) > Abs( (y+sy)*dx ) Or (Abs( (x+sx)*dy ) = Abs( (y+sy)*dx ) And checkAlt=0)
		
			If curr\dullhorse[2+sy] = Null
				Exit
			Else
				curr = curr\dullhorse[2+sy]
				
				If nonjumping = 1
					If curr\p <> Null
						Exit
					EndIf
				EndIf
				
				y = y+sy
			EndIf
			
		EndIf
	
	Wend
	
	If x = dx And y = dy
		dests\c = curr
	EndIf
		
	If checkAlt = 1 ;if board is ramifying, order matters, so check the other route
		dests2.celloplayer = destinations.celloplayer(c, dx, dy, 0, nonjumping)
		
		If dests2\c <> Null ;alternate found
			If dests\c = Null ;original not found
				dests\c = dests2\c ;"replace"
			ElseIf dests2\c <> dests\c ;different cells
				dests\nex = dests2 ;add to list
				dests2\bef = dests
			EndIf
		EndIf
	EndIf
	
	Return dests

End Function

Function fetchPlayerById.player(id)

	For pl.player = Each player
		If pl\id = id
			Return pl
		EndIf
	Next
	
End Function

Function playerInList(pl.playerlist, p.player)

	If pl = Null
		Return 0
	EndIf

	found = 0
	
	If pl\p = p
		Return 1
	EndIf
	
	hold.player = pl\p
	
	While pl\bef <> Null
		If pl\p = p
			found = 1
			Exit
		EndIf
		
		pl = pl\bef
	Wend
	
	While pl\p <> hold
		pl = pl\nex
	Wend
	
	If found = 0
		While pl <> Null
			If pl\p = p
				found = 1
				Exit
			EndIf
		
			If pl\nex <> Null
				pl = pl\nex
			Else
				Exit
			EndIf
		Wend
	EndIf
	
	While pl\p <> hold
		pl = pl\bef
	Wend
	
	Return found

End Function

Function appendPlayer(pl.playerlist, p.player)

	If pl = Null
		pl = New playerlist
		pl\p = p
		Return
	EndIf

	hold.player = pl\p

	While pl <> Null
		If pl\nex <> Null
			pl = pl\nex
		Else
			Exit
		EndIf
	Wend
	
	pl\nex = New playerlist
	pl\nex\p = p
	pl\nex\bef = pl
	
	While pl\p <> hold
		pl = pl\bef
	Wend

End Function

Function fetchCellById.cell(id)

	For c.cell = Each cell
		If c\id = id
			Return c
		EndIf
	Next
	
	Return Null

End Function

Function cellInList(cl.celloplayer, c.cell)

	If cl = Null
		Return 0
	EndIf

	found = 0
	
	If cl\c = c
		Return 1
	EndIf
	
	hold.cell = cl\c
	
	While cl\bef <> Null
		If cl\c = c
			found = 1
			Exit
		EndIf
		
		cl = cl\bef
	Wend
	
	While cl\c <> hold
		cl = cl\nex
	Wend
	
	If found = 0
		While cl <> Null
			If cl\c = c
				found = 1
				Exit
			EndIf
		
			If cl\nex <> Null
				cl = cl\nex
			Else
				Exit
			EndIf
		Wend
	EndIf
	
	While cl\c <> hold
		cl = cl\bef
	Wend
	
	Return found

End Function

Function cellInList_cml.cellMoveList(cl.cellMoveList, c.cell)

	If cl = Null
		Return Null
	EndIf

	If cl\c = c
		Return cl
	EndIf

	While cl\bef <> Null
		cl = cl\bef
		If cl\c = c
			Return cl
		EndIf
	Wend
	
	While cl <> Null
		If cl\c = c
			Return cl
		EndIf
		cl = cl\nex
	Wend

	Return Null
	
End Function

Function appendCell(cl.celloplayer, c.cell)

	If cl = Null
		cl = New celloplayer
		cl\c = c
		Return
	EndIf

	hold.cell = cl\c

	While cl <> Null
		If cl\nex <> Null
			cl = cl\nex
		Else
			Exit
		EndIf
	Wend
	
	cl\nex = New celloplayer
	cl\nex\c = c
	cl\nex\bef = cl
	
	While cl\c <> hold
		cl = cl\bef
	Wend

End Function

Function fetchPieceById.piece(id)

	For p.piece = Each piece
		If p\id = id
			Return p
		EndIf
	Next
	
	Return Null

End Function

Function copyPiece.piece(p.piece)

	cp.piece = New piece
	
	cp\x = p\x
	cp\y = p\y
	cp\img = p\img
	cp\c = p\c
	cp\m = p\m
	cp\mobile = p\mobile
	cp\alive = p\alive
	cp\name = p\name
	cp\id = p\id
	cp\players = p\players
	;cp\players = copyPlayers(p\players)
	
	Return cp

End Function

Function fetchMoveById.move(id)

	For m.move = Each move
		If m\id = id
			Return m
		EndIf
	Next
	
	Return Null

End Function

Function moveInList(ml.movelist, m.move)

	found = 0
	
	If ml\m = m
		Return 1
	EndIf
	
	hold.move = ml\m
	
	While ml\bef <> Null
		If ml\m = m
			found = 1
			Exit
		EndIf
		
		ml = ml\bef
	Wend
	
	While ml\m <> hold
		ml = ml\nex
	Wend
	
	If found = 0
		While ml <> Null
			If ml\m = m
				found = 1
				Exit
			EndIf
		
			If ml\nex <> Null
				ml = ml\nex
			Else
				Exit
			EndIf
		Wend
	EndIf
	
	While ml\m <> hold
		ml = ml\bef
	Wend
	
	Return found

End Function

Function appendMove(ml.movelist, m.move)

	hold.move = ml\m

	While ml <> Null
		If ml\nex <> Null
		
			ml = ml\nex
		Else
			Exit
		EndIf
	Wend
	
	ml\nex = New movelist
	ml\nex\m = m
	ml\nex\bef = ml
	
	While ml\m <> hold
		ml = ml\bef
	Wend

End Function

Function removeMove(ml.moveList, moveId)

	;assumption: ml is at the beginning of the move list

	If ml\m\id = moveId ;if it's the first item
		If ml\nex <> Null
			ml = ml\nex
			Delete ml\bef
		Else
			Delete ml
		EndIf
		Return 1
	EndIf
	
	success = 0
	
	While ml\nex <> Null
		If ml\m\id = moveId
			;we know this is neither the first item nor the last
			ml\bef\nex = ml\nex
			ml\nex\bef = ml\bef
			success = 1
		EndIf
		ml = ml\nex
	Wend
	
	If ml\m\id = moveId ;if it's the last item
		ml = ml\bef
		ml\nex = Null
		success = 1
	EndIf
	
	;end condition: ml is at the beginning of the modified list
	While ml\bef <> Null
		ml = ml\bef
	Wend
	
	Return success

End Function

Function clearCellAlts()

	For c.cell = Each cell
		c\alt = 0
	Next

End Function

Function clearCellMoveList(cml.cellMoveList)

	While cml <> Null
		If cml\nex <> Null
			cml = cml\nex
			Delete cml\bef
		Else
			Delete cml
		EndIf
	Wend

End Function

;mouse functions
.mouseFuncs
Function checkInput(m.mouse)

	change = 0
	m\hit = 0

	If MouseHit(1)
		m\hit = 1
		
		If m\selector = 0 ;no cell has been selected
				
			For c.cell = Each cell
				If c\img <> 0
					If ImageRectCollide(c\img, c\x, c\y, 0, MouseX(), MouseY(), 1, 1)
						m\c = c
						m\selector = 1
						change = 1
						Exit
					EndIf
				EndIf
			Next
		
		ElseIf m\selector = 1 ;a cell has been selected
		
			;DebugLog "yes"
		
			For c.cell = Each cell
				If c\img <> 0
					If ImageRectCollide(c\img, c\x, c\y, 0, MouseX(), MouseY(), 1, 1)
						If m\c = c
							m\selector = 3
						Else
							m\c = c
							m\selector = 2
						EndIf
						change = 1
						Exit
					EndIf
				EndIf
			Next
		
		EndIf
	EndIf
	
	If MouseHit(2)
		m\hit = 2
		change = 1
	EndIf
	
	Return change

End Function

;miscellaneous
.miscellFuncs

Function checkSpecials(name$, b.board, undoSpecial=0)

	If name = "en passant" ;;;
	
		If undoSpecial = 1 ;likely called from the undo function
			If b\prevMoves\nex <> Null
				b\prevMoves = b\prevMoves\nex
			EndIf
		EndIf
	
		If b\prevMoves\m <> Null
			If b\prevMoves\m\id = 101 Or b\prevMoves\m\id = 102 ;white/black pawn double-moves
			
				enemyId = 103-b\prevMoves\m\id ;2 (black) if white moves and vice versa
				leftSide = 2*(b\prevMoves\m\id-100)
				rightSide = 6-leftSide
				leftMoveId = 103 + leftSide
				rightMoveId = leftMoveId + 1
				
				If b\prevMoves\destCell\dullhorse[leftSide] <> Null ;the cell exists
					If b\prevMoves\destCell\dullhorse[leftSide]\p <> Null ;there's a piece next to it
						If b\prevMoves\destCell\dullhorse[leftSide]\p\id = enemyId ;black pawn
							If undoSpecial = 0 ;if I'm not undoing, add the move. Delete it otherwise.
								appendMove(b\prevMoves\destCell\dullhorse[leftSide]\p\m, fetchMoveById(leftMoveId))
							Else
								removeMove(b\prevMoves\destCell\dullhorse[leftSide]\p\m, leftMoveId)
							EndIf
						EndIf
					EndIf
				EndIf
				
				If b\prevMoves\destCell\dullhorse[rightSide] <> Null ;the cell exists
					If b\prevMoves\destCell\dullhorse[rightSide]\p <> Null ;there's a piece next to it
						If b\prevMoves\destCell\dullhorse[rightSide]\p\id = enemyId ;black pawn
							If undoSpecial = 0 ;if I'm not undoing, add the move. Delete it otherwise.
								appendMove(b\prevMoves\destCell\dullhorse[rightSide]\p\m, fetchMoveById(rightMoveId))
							Else
								removeMove(b\prevMoves\destCell\dullhorse[rightSide]\p\m, rightMoveId)
							EndIf
						EndIf
					EndIf
				EndIf
				
			ElseIf b\prevMoves\m\id >= 105 And b\prevMoves\m\id <= 108 ;en-passanted
				
				If b\prevMoves\capId = 0 ;if the capture hasn't yet been made
					nORs = 1 + 2*Int( (108-b\prevMoves\m\id)/2 )
				
					If b\prevMoves\destCell\dullhorse[nORs]\p <> Null ;make the capture
						b\prevMoves\capId = b\prevMoves\destCell\dullhorse[nORs]\p\id
						b\prevMoves\capx = b\prevMoves\destCell\dullhorse[nORs]\px
						b\prevMoves\capy = b\prevMoves\destCell\dullhorse[nORs]\py
						b\prevMoves\capP = b\prevMoves\destCell\dullhorse[nORs]\p
						b\prevMoves\capCell = b\prevMoves\destCell\dullhorse[nORs]
						b\prevMoves\capprevmvs = b\prevMoves\destCell\dullhorse[nORs]\p\m
						
						b\prevMoves\destCell\dullhorse[nORs]\p = Null
					Else
						DebugLog "Uh-oh! You done goofed!"
					EndIf
				EndIf
				
			EndIf
		EndIf
		
		If undoSpecial = 1
			b\prevMoves = b\prevMoves\bef
		EndIf
	
	ElseIf name = "castling" ;;;
		
		If undoSpecial = 1
			If b\prevMoves\nex <> Null
				b\prevMoves = b\prevMoves\nex
			Else
				Return
			EndIf
		EndIf
		
		If b\prevMoves\m <> Null
		
			If b\prevMoves\m\id >= 11 And b\prevMoves\m\id <= 14 ;a rook moved
			
				;but which one?
				;check to the west first
				c.cell = b\prevMoves\srcCell
				While c\dullhorse[4] <> Null
					If c\dullhorse[4]\p <> Null
						If (c\dullhorse[4]\p\id = 11 And b\prevMoves\p\id = 7) Or (c\dullhorse[4]\p\id = 12 And b\prevMoves\p\id = 8) ;white king/rook or black king/rook
							
							If undoSpecial = 0
								removeMove(c\dullhorse[4]\p\m, 111)
							Else
								appendMove(c\dullhorse[4]\p\m, fetchMoveById(111))
							EndIf
							
						EndIf
						Exit
					EndIf
					c = c\dullhorse[4]
				Wend
				
				;now check to the east
				c = b\prevMoves\srcCell
				While c\dullhorse[2] <> Null
					If c\dullhorse[2]\p <> Null
						If (c\dullhorse[2]\p\id = 11 And b\prevMoves\p\id = 7) Or (c\dullhorse[2]\p\id = 12 And b\prevMoves\p\id = 8) ;white king/rook or black king/rook
							
							If undoSpecial = 0
								removeMove(c\dullhorse[2]\p\m, 110)
							Else
								appendMove(c\dullhorse[2]\p\m, fetchMoveById(110))
							EndIf
							
						EndIf
						Exit
					EndIf
					c = c\dullhorse[2]
				Wend
			
			ElseIf (b\prevMoves\m\id = 110 Or b\prevMoves\m\id = 111) And undoSpecial=0 ;castling has happened
			
				eORw = 6 - 2*(b\prevMoves\m\id - 109) ;2 if 110, 4 if 111
				
				c.cell = b\prevMoves\destCell
				
				While c\dullhorse[eORw] <> Null
					
					If c\dullhorse[eORw]\p <> Null
						If c\dullhorse[eORw]\p\id = b\prevMoves\p\id-4
						
							;DebugLog c\dullhorse[eORw]\id+" : "+b\prevMoves\destCell\id+" : "+b\prevMoves\destCell\dullhorse[6-eORw]\id
							
							b\prevMoves\other = New historyList
							
							movePiece(c\dullhorse[eORw], b\prevMoves\destCell\dullhorse[6-eORw], b\prevMoves\other)
							
							;b\prevMoves\other\destCell\px = b\prevMoves\other\srcCell\px
							;b\prevMoves\other\destCell\py = b\prevMoves\other\srcCell\py
							;b\prevMoves\other\destCell\p  = b\prevMoves\other\srcCell\p
							;Stop
							
							;hist\m = cml\srcmv
							;b\prevMoves\other\m = c\dullhorse[eORw]\p\m
							;b\prevMoves\other\prevmvs = c\dullhorse[eORw]\p\m
							;hist\prevmvs = g\m\c\p\m
							;hist\playa = g\b\current
							
							Exit
							
						EndIf
					EndIf
					
					c = c\dullhorse[eORw]
					
				Wend
			
			EndIf
		
		EndIf
		
		If undoSpecial = 1
			b\prevMoves = b\prevMoves\bef
		EndIf
	
	Else
		DebugLog "That special does not exist."
	EndIf

End Function

Function checkAllSpecials(b.board, undoSpecial=0)

	speshes.specialsList = b\specials

	While speshes <> Null ;loop through the board's specials
	
		checkSpecials(speshes\s\name, b, undoSpecial) ;check the special moves
		
		If speshes\nex = Null
			Exit
		EndIf
		speshes = speshes\nex
	Wend

End Function

; navigation ;