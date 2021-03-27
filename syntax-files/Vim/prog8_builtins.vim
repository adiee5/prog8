" Vim syntax file
" Language: Prog8 (built-in functions)
" Maintainer: Elektron72
" Latest Revision: 23 March 2021


" Built-in functions

" Math functions
syn keyword prog8BuiltInFunc abs atan ceil cos cos8u cos8 cos16u cos16 deg floor
syn keyword prog8BuiltInFunc ln log2 rad round sin sgn sin8u sin8 sin16u sin16
syn keyword prog8BuiltInFunc sqrt16 sqrt tan

" Array functions
syn keyword prog8BuiltInFunc any all len max min reverse sum sort

" Miscellaneous functions
syn keyword prog8BuiltInFunc cmp lsb msb mkword peek peekw poke pokew rnd rndw
syn keyword prog8BuiltInFunc rndf fastrnd8 rol rol2 ror ror2 sizeof offsetof
syn keyword prog8BuiltInFunc swap memory


" c64/floats.p8
syn match prog8BuiltInVar "\<floats\.PI\>"
syn match prog8BuiltInVar "\<floats\.TWOPI\>"
syn match prog8BuiltInFunc "\<floats\.MOVFM\>"
syn match prog8BuiltInFunc "\<floats\.FREADMEM\>"
syn match prog8BuiltInFunc "\<floats\.CONUPK\>"
syn match prog8BuiltInFunc "\<floats\.FAREADMEM\>"
syn match prog8BuiltInFunc "\<floats\.MOVFA\>"
syn match prog8BuiltInFunc "\<floats\.MOVAF\>"
syn match prog8BuiltInFunc "\<floats\.MOVEF\>"
syn match prog8BuiltInFunc "\<floats\.MOVMF\>"
syn match prog8BuiltInFunc "\<floats\.FTOSWORDYA\>"
syn match prog8BuiltInFunc "\<floats\.GETADR\>"
syn match prog8BuiltInFunc "\<floats\.QINT\>"
syn match prog8BuiltInFunc "\<floats\.AYINT\>"
syn match prog8BuiltInFunc "\<floats\.GIVAYF\>"
syn match prog8BuiltInFunc "\<floats\.FREADUY\>"
syn match prog8BuiltInFunc "\<floats\.FREADSA\>"
syn match prog8BuiltInFunc "\<floats\.FREADSTR\>"
syn match prog8BuiltInFunc "\<floats\.FPRINTLN\>"
syn match prog8BuiltInFunc "\<floats\.FOUT\>"
syn match prog8BuiltInFunc "\<floats\.FADDH\>"
syn match prog8BuiltInFunc "\<floats\.MUL10\>"
syn match prog8BuiltInFunc "\<floats\.DIV10\>"
syn match prog8BuiltInFunc "\<floats\.FCOMP\>"
syn match prog8BuiltInFunc "\<floats\.FADDT\>"
syn match prog8BuiltInFunc "\<floats\.FADD\>"
syn match prog8BuiltInFunc "\<floats\.FSUBT\>"
syn match prog8BuiltInFunc "\<floats\.FSUB\>"
syn match prog8BuiltInFunc "\<floats\.FMULTT\>"
syn match prog8BuiltInFunc "\<floats\.FMULT\>"
syn match prog8BuiltInFunc "\<floats\.FDIVT\>"
syn match prog8BuiltInFunc "\<floats\.FDIV\>"
syn match prog8BuiltInFunc "\<floats\.FPWRT\>"
syn match prog8BuiltInFunc "\<floats\.FPWR\>"
syn match prog8BuiltInFunc "\<floats\.FINLOG\>"
syn match prog8BuiltInFunc "\<floats\.NOTOP\>"
syn match prog8BuiltInFunc "\<floats\.INT\>"
syn match prog8BuiltInFunc "\<floats\.LOG\>"
syn match prog8BuiltInFunc "\<floats\.SGN\>"
syn match prog8BuiltInFunc "\<floats\.SIGN\>"
syn match prog8BuiltInFunc "\<floats\.ABS\>"
syn match prog8BuiltInFunc "\<floats\.SQR\>"
syn match prog8BuiltInFunc "\<floats\.SQRA\>"
syn match prog8BuiltInFunc "\<floats\.EXP\>"
syn match prog8BuiltInFunc "\<floats\.NEGOP\>"
syn match prog8BuiltInFunc "\<floats\.RND\>"
syn match prog8BuiltInFunc "\<floats\.COS\>"
syn match prog8BuiltInFunc "\<floats\.SIN\>"
syn match prog8BuiltInFunc "\<floats\.TAN\>"
syn match prog8BuiltInFunc "\<floats\.ATN\>"
syn match prog8BuiltInFunc "\<floats\.FREADS32\>"
syn match prog8BuiltInFunc "\<floats\.FREADUS32\>"
syn match prog8BuiltInFunc "\<floats\.FREADS24AXY\>"
syn match prog8BuiltInFunc "\<floats\.GIVUAYFAY\>"
syn match prog8BuiltInFunc "\<floats\.GIVAFAY\>"
syn match prog8BuiltInFunc "\<floats\.FTOSWRDAY\>"
syn match prog8BuiltInFunc "\<floats\.GETADRAY\>"
syn match prog8BuiltInFunc "\<floats\.print_f\>"


" c64/graphics.p8
syn match prog8BuiltInVar "\<graphics\.BITMAP_ADDRESS\>"
syn match prog8BuiltInVar "\<graphics\.WIDTH\>"
syn match prog8BuiltInVar "\<graphics\.HEIGHT\>"
syn match prog8BuiltInFunc "\<graphics\.enable_bitmap_mode\>"
syn match prog8BuiltInFunc "\<graphics\.disable_bitmap_mode\>"
syn match prog8BuiltInFunc "\<graphics\.clear_screen\>"
syn match prog8BuiltInFunc "\<graphics\.line\>"
syn match prog8BuiltInFunc "\<graphics\.rect\>"
syn match prog8BuiltInFunc "\<graphics\.fillrect\>"
syn match prog8BuiltInFunc "\<graphics\.horizontal_line\>"
syn match prog8BuiltInFunc "\<graphics\.vertical_line\>"
syn match prog8BuiltInFunc "\<graphics\.circle\>"
syn match prog8BuiltInFunc "\<graphics\.disc\>"
syn match prog8BuiltInFunc "\<graphics\.plot\>"


" c64/syslib.p8
syn match prog8BuiltInVar "\<c64\.TIME_HI\>"
syn match prog8BuiltInVar "\<c64\.TIME_MID\>"
syn match prog8BuiltInVar "\<c64\.TIME_LO\>"
syn match prog8BuiltInVar "\<c64\.STATUS\>"
syn match prog8BuiltInVar "\<c64\.STKEY\>"
syn match prog8BuiltInVar "\<c64\.SFDX\>"
syn match prog8BuiltInVar "\<c64\.COLOR\>"
syn match prog8BuiltInVar "\<c64\.HIBASE\>"
syn match prog8BuiltInVar "\<c64\.CINV\>"
syn match prog8BuiltInVar "\<c64\.NMI_VEC\>"
syn match prog8BuiltInVar "\<c64\.RESET_VEC\>"
syn match prog8BuiltInVar "\<c64\.IRQ_VEC\>"
syn match prog8BuiltInVar "\<c64\.SPRPTR0\>"
syn match prog8BuiltInVar "\<c64\.SPRPTR1\>"
syn match prog8BuiltInVar "\<c64\.SPRPTR2\>"
syn match prog8BuiltInVar "\<c64\.SPRPTR3\>"
syn match prog8BuiltInVar "\<c64\.SPRPTR4\>"
syn match prog8BuiltInVar "\<c64\.SPRPTR5\>"
syn match prog8BuiltInVar "\<c64\.SPRPTR6\>"
syn match prog8BuiltInVar "\<c64\.SPRPTR7\>"
syn match prog8BuiltInVar "\<c64\.SPRPTR\>"
syn match prog8BuiltInVar "\<c64\.SP0X\>"
syn match prog8BuiltInVar "\<c64\.SP0Y\>"
syn match prog8BuiltInVar "\<c64\.SP1X\>"
syn match prog8BuiltInVar "\<c64\.SP1Y\>"
syn match prog8BuiltInVar "\<c64\.SP2X\>"
syn match prog8BuiltInVar "\<c64\.SP2Y\>"
syn match prog8BuiltInVar "\<c64\.SP3X\>"
syn match prog8BuiltInVar "\<c64\.SP3Y\>"
syn match prog8BuiltInVar "\<c64\.SP4X\>"
syn match prog8BuiltInVar "\<c64\.SP4Y\>"
syn match prog8BuiltInVar "\<c64\.SP5X\>"
syn match prog8BuiltInVar "\<c64\.SP5Y\>"
syn match prog8BuiltInVar "\<c64\.SP6X\>"
syn match prog8BuiltInVar "\<c64\.SP6Y\>"
syn match prog8BuiltInVar "\<c64\.SP7X\>"
syn match prog8BuiltInVar "\<c64\.SP7Y\>"
syn match prog8BuiltInVar "\<c64\.SPXY\>"
syn match prog8BuiltInVar "\<c64\.SPXYW\>"
syn match prog8BuiltInVar "\<c64\.MSIGX\>"
syn match prog8BuiltInVar "\<c64\.SCROLY\>"
syn match prog8BuiltInVar "\<c64\.RASTER\>"
syn match prog8BuiltInVar "\<c64\.LPENX\>"
syn match prog8BuiltInVar "\<c64\.LPENY\>"
syn match prog8BuiltInVar "\<c64\.SPENA\>"
syn match prog8BuiltInVar "\<c64\.SCROLX\>"
syn match prog8BuiltInVar "\<c64\.YXPAND\>"
syn match prog8BuiltInVar "\<c64\.VMCSB\>"
syn match prog8BuiltInVar "\<c64\.VICIRQ\>"
syn match prog8BuiltInVar "\<c64\.IREQMASK\>"
syn match prog8BuiltInVar "\<c64\.SPBGPR\>"
syn match prog8BuiltInVar "\<c64\.SPMC\>"
syn match prog8BuiltInVar "\<c64\.XXPAND\>"
syn match prog8BuiltInVar "\<c64\.SPSPCL\>"
syn match prog8BuiltInVar "\<c64\.SPBGCL\>"
syn match prog8BuiltInVar "\<c64\.EXTCOL\>"
syn match prog8BuiltInVar "\<c64\.BGCOL0\>"
syn match prog8BuiltInVar "\<c64\.BGCOL1\>"
syn match prog8BuiltInVar "\<c64\.BGCOL2\>"
syn match prog8BuiltInVar "\<c64\.BGCOL4\>"
syn match prog8BuiltInVar "\<c64\.SPMC0\>"
syn match prog8BuiltInVar "\<c64\.SPMC1\>"
syn match prog8BuiltInVar "\<c64\.SP0COL\>"
syn match prog8BuiltInVar "\<c64\.SP1COL\>"
syn match prog8BuiltInVar "\<c64\.SP2COL\>"
syn match prog8BuiltInVar "\<c64\.SP3COL\>"
syn match prog8BuiltInVar "\<c64\.SP4COL\>"
syn match prog8BuiltInVar "\<c64\.SP5COL\>"
syn match prog8BuiltInVar "\<c64\.SP6COL\>"
syn match prog8BuiltInVar "\<c64\.SP7COL\>"
syn match prog8BuiltInVar "\<c64\.SPCOL\>"
syn match prog8BuiltInVar "\<c64\.CIA1PRA\>"
syn match prog8BuiltInVar "\<c64\.CIA1PRB\>"
syn match prog8BuiltInVar "\<c64\.CIA1DDRA\>"
syn match prog8BuiltInVar "\<c64\.CIA1DDRB\>"
syn match prog8BuiltInVar "\<c64\.CIA1TAL\>"
syn match prog8BuiltInVar "\<c64\.CIA1TAH\>"
syn match prog8BuiltInVar "\<c64\.CIA1TBL\>"
syn match prog8BuiltInVar "\<c64\.CIA1TBH\>"
syn match prog8BuiltInVar "\<c64\.CIA1TOD10\>"
syn match prog8BuiltInVar "\<c64\.CIA1TODHR\>"
syn match prog8BuiltInVar "\<c64\.CIA1SDR\>"
syn match prog8BuiltInVar "\<c64\.CIA1ICR\>"
syn match prog8BuiltInVar "\<c64\.CIA1CRA\>"
syn match prog8BuiltInVar "\<c64\.CIA1CRB\>"
syn match prog8BuiltInVar "\<c64\.CIA2PRA\>"
syn match prog8BuiltInVar "\<c64\.CIA2PRB\>"
syn match prog8BuiltInVar "\<c64\.CIA2DDRA\>"
syn match prog8BuiltInVar "\<c64\.CIA2DDRB\>"
syn match prog8BuiltInVar "\<c64\.CIA2TAL\>"
syn match prog8BuiltInVar "\<c64\.CIA2TAH\>"
syn match prog8BuiltInVar "\<c64\.CIA2TBL\>"
syn match prog8BuiltInVar "\<c64\.CIA2TBH\>"
syn match prog8BuiltInVar "\<c64\.CIA2TOD10\>"
syn match prog8BuiltInVar "\<c64\.CIA2TODHR\>"
syn match prog8BuiltInVar "\<c64\.CIA2SDR\>"
syn match prog8BuiltInVar "\<c64\.CIA2ICR\>"
syn match prog8BuiltInVar "\<c64\.CIA2CRA\>"
syn match prog8BuiltInVar "\<c64\.CIA2CRB\>"
syn match prog8BuiltInVar "\<c64\.FREQLO1\>"
syn match prog8BuiltInVar "\<c64\.FREQHI1\>"
syn match prog8BuiltInVar "\<c64\.FREQ1\>"
syn match prog8BuiltInVar "\<c64\.PWLO1\>"
syn match prog8BuiltInVar "\<c64\.PWHI1\>"
syn match prog8BuiltInVar "\<c64\.PW1\>"
syn match prog8BuiltInVar "\<c64\.CR1\>"
syn match prog8BuiltInVar "\<c64\.AD1\>"
syn match prog8BuiltInVar "\<c64\.SR1\>"
syn match prog8BuiltInVar "\<c64\.FREQLO2\>"
syn match prog8BuiltInVar "\<c64\.FREQHI2\>"
syn match prog8BuiltInVar "\<c64\.FREQ2\>"
syn match prog8BuiltInVar "\<c64\.PWLO2\>"
syn match prog8BuiltInVar "\<c64\.PWHI2\>"
syn match prog8BuiltInVar "\<c64\.PW2\>"
syn match prog8BuiltInVar "\<c64\.CR2\>"
syn match prog8BuiltInVar "\<c64\.AD2\>"
syn match prog8BuiltInVar "\<c64\.SR2\>"
syn match prog8BuiltInVar "\<c64\.FREQLO3\>"
syn match prog8BuiltInVar "\<c64\.FREQHI3\>"
syn match prog8BuiltInVar "\<c64\.FREQ3\>"
syn match prog8BuiltInVar "\<c64\.PWLO3\>"
syn match prog8BuiltInVar "\<c64\.PWHI3\>"
syn match prog8BuiltInVar "\<c64\.PW3\>"
syn match prog8BuiltInVar "\<c64\.CR3\>"
syn match prog8BuiltInVar "\<c64\.AD3\>"
syn match prog8BuiltInVar "\<c64\.SR3\>"
syn match prog8BuiltInVar "\<c64\.FCLO\>"
syn match prog8BuiltInVar "\<c64\.FCHI\>"
syn match prog8BuiltInVar "\<c64\.FC\>"
syn match prog8BuiltInVar "\<c64\.RESFILT\>"
syn match prog8BuiltInVar "\<c64\.MVOL\>"
syn match prog8BuiltInVar "\<c64\.POTX\>"
syn match prog8BuiltInVar "\<c64\.POTY\>"
syn match prog8BuiltInVar "\<c64\.OSC3\>"
syn match prog8BuiltInVar "\<c64\.ENV3\>"
syn match prog8BuiltInFunc "\<c64\.STROUT\>"
syn match prog8BuiltInFunc "\<c64\.CLEARSCR\>"
syn match prog8BuiltInFunc "\<c64\.HOMECRSR\>"
syn match prog8BuiltInFunc "\<c64\.IRQDFRT\>"
syn match prog8BuiltInFunc "\<c64\.IRQDFEND\>"
syn match prog8BuiltInFunc "\<c64\.CINT\>"
syn match prog8BuiltInFunc "\<c64\.IOINIT\>"
syn match prog8BuiltInFunc "\<c64\.RAMTAS\>"
syn match prog8BuiltInFunc "\<c64\.RESTOR\>"
syn match prog8BuiltInFunc "\<c64\.VECTOR\>"
syn match prog8BuiltInFunc "\<c64\.SETMSG\>"
syn match prog8BuiltInFunc "\<c64\.SECOND\>"
syn match prog8BuiltInFunc "\<c64\.TKSA\>"
syn match prog8BuiltInFunc "\<c64\.MEMTOP\>"
syn match prog8BuiltInFunc "\<c64\.MEMBOT\>"
syn match prog8BuiltInFunc "\<c64\.SCNKEY\>"
syn match prog8BuiltInFunc "\<c64\.SETTMO\>"
syn match prog8BuiltInFunc "\<c64\.ACPTR\>"
syn match prog8BuiltInFunc "\<c64\.CIOUT\>"
syn match prog8BuiltInFunc "\<c64\.UNTLK\>"
syn match prog8BuiltInFunc "\<c64\.UNLSN\>"
syn match prog8BuiltInFunc "\<c64\.LISTEN\>"
syn match prog8BuiltInFunc "\<c64\.TALK\>"
syn match prog8BuiltInFunc "\<c64\.READST\>"
syn match prog8BuiltInFunc "\<c64\.SETLFS\>"
syn match prog8BuiltInFunc "\<c64\.SETNAM\>"
syn match prog8BuiltInFunc "\<c64\.OPEN\>"
syn match prog8BuiltInFunc "\<c64\.CLOSE\>"
syn match prog8BuiltInFunc "\<c64\.CHKIN\>"
syn match prog8BuiltInFunc "\<c64\.CHKOUT\>"
syn match prog8BuiltInFunc "\<c64\.CLRCHN\>"
syn match prog8BuiltInFunc "\<c64\.CHRIN\>"
syn match prog8BuiltInFunc "\<c64\.CHROUT\>"
syn match prog8BuiltInFunc "\<c64\.LOAD\>"
syn match prog8BuiltInFunc "\<c64\.SAVE\>"
syn match prog8BuiltInFunc "\<c64\.SETTIM\>"
syn match prog8BuiltInFunc "\<c64\.RDTIM\>"
syn match prog8BuiltInFunc "\<c64\.STOP\>"
syn match prog8BuiltInFunc "\<c64\.GETIN\>"
syn match prog8BuiltInFunc "\<c64\.CLALL\>"
syn match prog8BuiltInFunc "\<c64\.UDTIM\>"
syn match prog8BuiltInFunc "\<c64\.SCREEN\>"
syn match prog8BuiltInFunc "\<c64\.PLOT\>"
syn match prog8BuiltInFunc "\<c64\.IOBASE\>"
syn match prog8BuiltInFunc "\<c64\.STOP2\>"
syn match prog8BuiltInFunc "\<c64\.RDTIM16\>"
syn match prog8BuiltInFunc "\<c64\.init_system\>"
syn match prog8BuiltInFunc "\<c64\.init_system_phase2\>"
syn match prog8BuiltInFunc "\<c64\.disable_runstop_and_charsetswitch\>"
syn match prog8BuiltInFunc "\<c64\.set_irq\>"
syn match prog8BuiltInFunc "\<c64\.restore_irq\>"
syn match prog8BuiltInFunc "\<c64\.set_rasterirq\>"
syn match prog8BuiltInVar "\<sys\.target\>"
syn match prog8BuiltInFunc "\<sys\.reset_system\>"
syn match prog8BuiltInFunc "\<sys\.wait\>"
syn match prog8BuiltInFunc "\<sys\.memcopy\>"
syn match prog8BuiltInFunc "\<sys\.memset\>"
syn match prog8BuiltInFunc "\<sys\.memsetw\>"
syn match prog8BuiltInFunc "\<sys\.rsave\>"
syn match prog8BuiltInFunc "\<sys\.rrestore\>"
syn match prog8BuiltInFunc "\<sys\.read_flags\>"
syn match prog8BuiltInFunc "\<sys\.clear_carry\>"
syn match prog8BuiltInFunc "\<sys\.set_carry\>"
syn match prog8BuiltInFunc "\<sys\.clear_irqd\>"
syn match prog8BuiltInFunc "\<sys\.set_irqd\>"
syn match prog8BuiltInFunc "\<sys\.exit\>"
syn match prog8BuiltInFunc "\<sys\.progend\>"
syn match prog8BuiltInVar "\<cx16\.r0\>"
syn match prog8BuiltInVar "\<cx16\.r1\>"
syn match prog8BuiltInVar "\<cx16\.r2\>"
syn match prog8BuiltInVar "\<cx16\.r3\>"
syn match prog8BuiltInVar "\<cx16\.r4\>"
syn match prog8BuiltInVar "\<cx16\.r5\>"
syn match prog8BuiltInVar "\<cx16\.r6\>"
syn match prog8BuiltInVar "\<cx16\.r7\>"
syn match prog8BuiltInVar "\<cx16\.r8\>"
syn match prog8BuiltInVar "\<cx16\.r9\>"
syn match prog8BuiltInVar "\<cx16\.r10\>"
syn match prog8BuiltInVar "\<cx16\.r11\>"
syn match prog8BuiltInVar "\<cx16\.r12\>"
syn match prog8BuiltInVar "\<cx16\.r13\>"
syn match prog8BuiltInVar "\<cx16\.r14\>"
syn match prog8BuiltInVar "\<cx16\.r15\>"


" c64/textio.p8
syn match prog8BuiltInVar "\<txt\.DEFAULT_WIDTH\>"
syn match prog8BuiltInVar "\<txt\.DEFAULT_HEIGHT\>"
syn match prog8BuiltInFunc "\<txt\.clear_screen\>"
syn match prog8BuiltInFunc "\<txt\.home\>"
syn match prog8BuiltInFunc "\<txt\.nl\>"
syn match prog8BuiltInFunc "\<txt\.spc\>"
syn match prog8BuiltInFunc "\<txt\.column\>"
syn match prog8BuiltInFunc "\<txt\.fill_screen\>"
syn match prog8BuiltInFunc "\<txt\.clear_screenchars\>"
syn match prog8BuiltInFunc "\<txt\.clear_screencolors\>"
syn match prog8BuiltInFunc "\<txt\.color\>"
syn match prog8BuiltInFunc "\<txt\.lowercase\>"
syn match prog8BuiltInFunc "\<txt\.uppercase\>"
syn match prog8BuiltInFunc "\<txt\.scroll_left\>"
syn match prog8BuiltInFunc "\<txt\.scroll_right\>"
syn match prog8BuiltInFunc "\<txt\.scroll_up\>"
syn match prog8BuiltInFunc "\<txt\.scroll_down\>"
syn match prog8BuiltInFunc "\<txt\.chrout\>"
syn match prog8BuiltInFunc "\<txt\.print\>"
syn match prog8BuiltInFunc "\<txt\.print_ub0\>"
syn match prog8BuiltInFunc "\<txt\.print_ub\>"
syn match prog8BuiltInFunc "\<txt\.print_b\>"
syn match prog8BuiltInFunc "\<txt\.print_ubhex\>"
syn match prog8BuiltInFunc "\<txt\.print_ubbin\>"
syn match prog8BuiltInFunc "\<txt\.print_uwbin\>"
syn match prog8BuiltInFunc "\<txt\.print_uwhex\>"
syn match prog8BuiltInFunc "\<txt\.print_uw0\>"
syn match prog8BuiltInFunc "\<txt\.print_uw\>"
syn match prog8BuiltInFunc "\<txt\.print_w\>"
syn match prog8BuiltInFunc "\<txt\.input_chars\>"
syn match prog8BuiltInFunc "\<txt\.setchr\>"
syn match prog8BuiltInFunc "\<txt\.getchr\>"
syn match prog8BuiltInFunc "\<txt\.setclr\>"
syn match prog8BuiltInFunc "\<txt\.getclr\>"
syn match prog8BuiltInFunc "\<txt\.setcc\>"
syn match prog8BuiltInFunc "\<txt\.plot\>"
syn match prog8BuiltInFunc "\<txt\.width\>"
syn match prog8BuiltInFunc "\<txt\.height\>"


" cx16/floats.p8
syn match prog8BuiltInVar "\<floats\.PI\>"
syn match prog8BuiltInVar "\<floats\.TWOPI\>"
syn match prog8BuiltInFunc "\<floats\.AYINT\>"
syn match prog8BuiltInFunc "\<floats\.GIVAYF\>"
syn match prog8BuiltInFunc "\<floats\.GETADR\>"
syn match prog8BuiltInFunc "\<floats\.FADDH\>"
syn match prog8BuiltInFunc "\<floats\.FSUB\>"
syn match prog8BuiltInFunc "\<floats\.FSUBT\>"
syn match prog8BuiltInFunc "\<floats\.FADD\>"
syn match prog8BuiltInFunc "\<floats\.FADDT\>"
syn match prog8BuiltInFunc "\<floats\.ZEROFC\>"
syn match prog8BuiltInFunc "\<floats\.NORMAL\>"
syn match prog8BuiltInFunc "\<floats\.LOG\>"
syn match prog8BuiltInFunc "\<floats\.FMULT\>"
syn match prog8BuiltInFunc "\<floats\.FMULTT\>"
syn match prog8BuiltInFunc "\<floats\.CONUPK\>"
syn match prog8BuiltInFunc "\<floats\.MUL10\>"
syn match prog8BuiltInFunc "\<floats\.DIV10\>"
syn match prog8BuiltInFunc "\<floats\.FDIV\>"
syn match prog8BuiltInFunc "\<floats\.FDIVT\>"
syn match prog8BuiltInFunc "\<floats\.MOVFM\>"
syn match prog8BuiltInFunc "\<floats\.MOVMF\>"
syn match prog8BuiltInFunc "\<floats\.MOVFA\>"
syn match prog8BuiltInFunc "\<floats\.MOVAF\>"
syn match prog8BuiltInFunc "\<floats\.MOVEF\>"
syn match prog8BuiltInFunc "\<floats\.SIGN\>"
syn match prog8BuiltInFunc "\<floats\.SGN\>"
syn match prog8BuiltInFunc "\<floats\.FREADSA\>"
syn match prog8BuiltInFunc "\<floats\.ABS\>"
syn match prog8BuiltInFunc "\<floats\.FCOMP\>"
syn match prog8BuiltInFunc "\<floats\.INT\>"
syn match prog8BuiltInFunc "\<floats\.FINLOG\>"
syn match prog8BuiltInFunc "\<floats\.FOUT\>"
syn match prog8BuiltInFunc "\<floats\.SQR\>"
syn match prog8BuiltInFunc "\<floats\.FPWRT\>"
syn match prog8BuiltInFunc "\<floats\.NEGOP\>"
syn match prog8BuiltInFunc "\<floats\.EXP\>"
syn match prog8BuiltInFunc "\<floats\.RND2\>"
syn match prog8BuiltInFunc "\<floats\.RND\>"
syn match prog8BuiltInFunc "\<floats\.COS\>"
syn match prog8BuiltInFunc "\<floats\.SIN\>"
syn match prog8BuiltInFunc "\<floats\.TAN\>"
syn match prog8BuiltInFunc "\<floats\.ATN\>"
syn match prog8BuiltInFunc "\<floats\.GIVUAYFAY\>"
syn match prog8BuiltInFunc "\<floats\.GIVAYFAY\>"
syn match prog8BuiltInFunc "\<floats\.FTOSWRDAY\>"
syn match prog8BuiltInFunc "\<floats\.GETADRAY\>"
syn match prog8BuiltInFunc "\<floats\.FREADUY\>"
syn match prog8BuiltInFunc "\<floats\.h\>"


" cx16/gfx2.p8
syn match prog8BuiltInVar "\<gfx2\.active_mode\>"
syn match prog8BuiltInVar "\<gfx2\.width\>"
syn match prog8BuiltInVar "\<gfx2\.height\>"
syn match prog8BuiltInVar "\<gfx2\.bpp\>"
syn match prog8BuiltInVar "\<gfx2\.monochrome_dont_stipple_flag\>"
syn match prog8BuiltInFunc "\<gfx2\.screen_mode\>"
syn match prog8BuiltInFunc "\<gfx2\.clear_screen\>"
syn match prog8BuiltInFunc "\<gfx2\.monochrome_stipple\>"
syn match prog8BuiltInFunc "\<gfx2\.rect\>"
syn match prog8BuiltInFunc "\<gfx2\.fillrect\>"
syn match prog8BuiltInFunc "\<gfx2\.horizontal_line\>"
syn match prog8BuiltInFunc "\<gfx2\.vertical_line\>"
syn match prog8BuiltInFunc "\<gfx2\.line\>"
syn match prog8BuiltInFunc "\<gfx2\.circle\>"
syn match prog8BuiltInFunc "\<gfx2\.disc\>"
syn match prog8BuiltInFunc "\<gfx2\.plot\>"
syn match prog8BuiltInFunc "\<gfx2\.position\>"
syn match prog8BuiltInFunc "\<gfx2\.next_pixel\>"
syn match prog8BuiltInFunc "\<gfx2\.next_pixels\>"
syn match prog8BuiltInFunc "\<gfx2\.set_8_pixels_from_bits\>"
syn match prog8BuiltInVar "\<gfx2\.charset_orig_bank\>"
syn match prog8BuiltInVar "\<gfx2\.charset_orig_addr\>"
syn match prog8BuiltInVar "\<gfx2\.charset_bank\>"
syn match prog8BuiltInVar "\<gfx2\.charset_addr\>"
syn match prog8BuiltInFunc "\<gfx2\.text_charset\>"
syn match prog8BuiltInFunc "\<gfx2\.text\>"
syn match prog8BuiltInFunc "\<gfx2\.cs_innerloop640\>"
syn match prog8BuiltInFunc "\<gfx2\.addr_mul_24_for_highres_4c\>"
syn match prog8BuiltInFunc "\<gfx2\.addr_mul_24_for_lores_256c\>"


" cx16/graphics.p8
syn match prog8BuiltInVar "\<graphics\.WIDTH\>"
syn match prog8BuiltInVar "\<graphics\.HEIGHT\>"
syn match prog8BuiltInFunc "\<graphics\.enable_bitmap_mode\>"
syn match prog8BuiltInFunc "\<graphics\.disable_bitmap_mode\>"
syn match prog8BuiltInFunc "\<graphics\.clear_screen\>"
syn match prog8BuiltInFunc "\<graphics\.line\>"
syn match prog8BuiltInFunc "\<graphics\.fillrect\>"
syn match prog8BuiltInFunc "\<graphics\.rect\>"
syn match prog8BuiltInFunc "\<graphics\.horizontal_line\>"
syn match prog8BuiltInFunc "\<graphics\.vertical_line\>"
syn match prog8BuiltInFunc "\<graphics\.circle\>"
syn match prog8BuiltInFunc "\<graphics\.disc\>"
syn match prog8BuiltInFunc "\<graphics\.plot\>"


" cx16/palette.p8
syn match prog8BuiltInVar "\<palette\.vera_palette_ptr\>"
syn match prog8BuiltInVar "\<palette\.c\>"
syn match prog8BuiltInFunc "\<palette\.set_color\>"
syn match prog8BuiltInFunc "\<palette\.set_rgb4\>"
syn match prog8BuiltInFunc "\<palette\.set_rgb\>"
syn match prog8BuiltInFunc "\<palette\.set_rgb8\>"
syn match prog8BuiltInFunc "\<palette\.set_monochrome\>"
syn match prog8BuiltInFunc "\<palette\.set_grayscale\>"
syn match prog8BuiltInVar "\<palette\.C64_colorpalette_dark\>"
syn match prog8BuiltInVar "\<palette\.C64_colorpalette_pepto\>"
syn match prog8BuiltInVar "\<palette\.C64_colorpalette_light\>"
syn match prog8BuiltInFunc "\<palette\.set_c64pepto\>"
syn match prog8BuiltInFunc "\<palette\.set_c64light\>"
syn match prog8BuiltInFunc "\<palette\.set_c64dark\>"

" cx16/syslib.p8
syn match prog8BuiltInFunc "\<c64\.CINT\>"
syn match prog8BuiltInFunc "\<c64\.IOINIT\>"
syn match prog8BuiltInFunc "\<c64\.RAMTAS\>"
syn match prog8BuiltInFunc "\<c64\.RESTOR\>"
syn match prog8BuiltInFunc "\<c64\.VECTOR\>"
syn match prog8BuiltInFunc "\<c64\.SETMSG\>"
syn match prog8BuiltInFunc "\<c64\.SECOND\>"
syn match prog8BuiltInFunc "\<c64\.TKSA\>"
syn match prog8BuiltInFunc "\<c64\.MEMTOP\>"
syn match prog8BuiltInFunc "\<c64\.MEMBOT\>"
syn match prog8BuiltInFunc "\<c64\.SCNKEY\>"
syn match prog8BuiltInFunc "\<c64\.SETTMO\>"
syn match prog8BuiltInFunc "\<c64\.ACPTR\>"
syn match prog8BuiltInFunc "\<c64\.CIOUT\>"
syn match prog8BuiltInFunc "\<c64\.UNTLK\>"
syn match prog8BuiltInFunc "\<c64\.UNLSN\>"
syn match prog8BuiltInFunc "\<c64\.LISTEN\>"
syn match prog8BuiltInFunc "\<c64\.TALK\>"
syn match prog8BuiltInFunc "\<c64\.READST\>"
syn match prog8BuiltInFunc "\<c64\.SETLFS\>"
syn match prog8BuiltInFunc "\<c64\.SETNAM\>"
syn match prog8BuiltInFunc "\<c64\.OPEN\>"
syn match prog8BuiltInFunc "\<c64\.CLOSE\>"
syn match prog8BuiltInFunc "\<c64\.CHKIN\>"
syn match prog8BuiltInFunc "\<c64\.CHKOUT\>"
syn match prog8BuiltInFunc "\<c64\.CLRCHN\>"
syn match prog8BuiltInFunc "\<c64\.CHRIN\>"
syn match prog8BuiltInFunc "\<c64\.CHROUT\>"
syn match prog8BuiltInFunc "\<c64\.LOAD\>"
syn match prog8BuiltInFunc "\<c64\.SAVE\>"
syn match prog8BuiltInFunc "\<c64\.SETTIM\>"
syn match prog8BuiltInFunc "\<c64\.RDTIM\>"
syn match prog8BuiltInFunc "\<c64\.STOP\>"
syn match prog8BuiltInFunc "\<c64\.GETIN\>"
syn match prog8BuiltInFunc "\<c64\.CLALL\>"
syn match prog8BuiltInFunc "\<c64\.UDTIM\>"
syn match prog8BuiltInFunc "\<c64\.SCREEN\>"
syn match prog8BuiltInFunc "\<c64\.PLOT\>"
syn match prog8BuiltInFunc "\<c64\.IOBASE\>"
syn match prog8BuiltInFunc "\<c64\.STOP2\>"
syn match prog8BuiltInFunc "\<c64\.RDTIM16\>"
syn match prog8BuiltInFunc "\<c64\.MEMTOP2\>"
syn match prog8BuiltInVar "\<cx16\.CINV\>"
syn match prog8BuiltInVar "\<cx16\.NMI_VEC\>"
syn match prog8BuiltInVar "\<cx16\.RESET_VEC\>"
syn match prog8BuiltInVar "\<cx16\.IRQ_VEC\>"
syn match prog8BuiltInVar "\<cx16\.r0\>"
syn match prog8BuiltInVar "\<cx16\.r1\>"
syn match prog8BuiltInVar "\<cx16\.r2\>"
syn match prog8BuiltInVar "\<cx16\.r3\>"
syn match prog8BuiltInVar "\<cx16\.r4\>"
syn match prog8BuiltInVar "\<cx16\.r5\>"
syn match prog8BuiltInVar "\<cx16\.r6\>"
syn match prog8BuiltInVar "\<cx16\.r7\>"
syn match prog8BuiltInVar "\<cx16\.r8\>"
syn match prog8BuiltInVar "\<cx16\.r9\>"
syn match prog8BuiltInVar "\<cx16\.r10\>"
syn match prog8BuiltInVar "\<cx16\.r11\>"
syn match prog8BuiltInVar "\<cx16\.r12\>"
syn match prog8BuiltInVar "\<cx16\.r13\>"
syn match prog8BuiltInVar "\<cx16\.r14\>"
syn match prog8BuiltInVar "\<cx16\.r15\>"
syn match prog8BuiltInVar "\<cx16\.VERA_BASE\>"
syn match prog8BuiltInVar "\<cx16\.VERA_ADDR_L\>"
syn match prog8BuiltInVar "\<cx16\.VERA_ADDR_M\>"
syn match prog8BuiltInVar "\<cx16\.VERA_ADDR_H\>"
syn match prog8BuiltInVar "\<cx16\.VERA_DATA0\>"
syn match prog8BuiltInVar "\<cx16\.VERA_DATA1\>"
syn match prog8BuiltInVar "\<cx16\.VERA_CTRL\>"
syn match prog8BuiltInVar "\<cx16\.VERA_IEN\>"
syn match prog8BuiltInVar "\<cx16\.VERA_ISR\>"
syn match prog8BuiltInVar "\<cx16\.VERA_IRQ_LINE_L\>"
syn match prog8BuiltInVar "\<cx16\.VERA_DC_VIDEO\>"
syn match prog8BuiltInVar "\<cx16\.VERA_DC_HSCALE\>"
syn match prog8BuiltInVar "\<cx16\.VERA_DC_VSCALE\>"
syn match prog8BuiltInVar "\<cx16\.VERA_DC_BORDER\>"
syn match prog8BuiltInVar "\<cx16\.VERA_DC_HSTART\>"
syn match prog8BuiltInVar "\<cx16\.VERA_DC_HSTOP\>"
syn match prog8BuiltInVar "\<cx16\.VERA_DC_VSTART\>"
syn match prog8BuiltInVar "\<cx16\.VERA_DC_VSTOP\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L0_CONFIG\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L0_MAPBASE\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L0_TILEBASE\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L0_HSCROLL_L\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L0_HSCROLL_H\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L0_VSCROLL_L\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L0_VSCROLL_H\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L1_CONFIG\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L1_MAPBASE\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L1_TILEBASE\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L1_HSCROLL_L\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L1_HSCROLL_H\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L1_VSCROLL_L\>"
syn match prog8BuiltInVar "\<cx16\.VERA_L1_VSCROLL_H\>"
syn match prog8BuiltInVar "\<cx16\.VERA_AUDIO_CTRL\>"
syn match prog8BuiltInVar "\<cx16\.VERA_AUDIO_RATE\>"
syn match prog8BuiltInVar "\<cx16\.VERA_AUDIO_DATA\>"
syn match prog8BuiltInVar "\<cx16\.VERA_SPI_DATA\>"
syn match prog8BuiltInVar "\<cx16\.VERA_SPI_CTRL\>"
syn match prog8BuiltInVar "\<cx16\.via1\>"
syn match prog8BuiltInVar "\<cx16\.d1prb\>"
syn match prog8BuiltInVar "\<cx16\.d1pra\>"
syn match prog8BuiltInVar "\<cx16\.d1ddrb\>"
syn match prog8BuiltInVar "\<cx16\.d1ddra\>"
syn match prog8BuiltInVar "\<cx16\.d1t1l\>"
syn match prog8BuiltInVar "\<cx16\.d1t1h\>"
syn match prog8BuiltInVar "\<cx16\.d1t1ll\>"
syn match prog8BuiltInVar "\<cx16\.d1t1lh\>"
syn match prog8BuiltInVar "\<cx16\.d1t2l\>"
syn match prog8BuiltInVar "\<cx16\.d1t2h\>"
syn match prog8BuiltInVar "\<cx16\.d1sr\>"
syn match prog8BuiltInVar "\<cx16\.d1acr\>"
syn match prog8BuiltInVar "\<cx16\.d1pcr\>"
syn match prog8BuiltInVar "\<cx16\.d1ifr\>"
syn match prog8BuiltInVar "\<cx16\.d1ier\>"
syn match prog8BuiltInVar "\<cx16\.d1ora\>"
syn match prog8BuiltInVar "\<cx16\.via2\>"
syn match prog8BuiltInVar "\<cx16\.d2prb\>"
syn match prog8BuiltInVar "\<cx16\.d2pra\>"
syn match prog8BuiltInVar "\<cx16\.d2ddrb\>"
syn match prog8BuiltInVar "\<cx16\.d2ddra\>"
syn match prog8BuiltInVar "\<cx16\.d2t1l\>"
syn match prog8BuiltInVar "\<cx16\.d2t1h\>"
syn match prog8BuiltInVar "\<cx16\.d2t1ll\>"
syn match prog8BuiltInVar "\<cx16\.d2t1lh\>"
syn match prog8BuiltInVar "\<cx16\.d2t2l\>"
syn match prog8BuiltInVar "\<cx16\.d2t2h\>"
syn match prog8BuiltInVar "\<cx16\.d2sr\>"
syn match prog8BuiltInVar "\<cx16\.d2acr\>"
syn match prog8BuiltInVar "\<cx16\.d2pcr\>"
syn match prog8BuiltInVar "\<cx16\.d2ifr\>"
syn match prog8BuiltInVar "\<cx16\.d2ier\>"
syn match prog8BuiltInVar "\<cx16\.d2ora\>"
syn match prog8BuiltInFunc "\<cx16\.close_all\>"
syn match prog8BuiltInFunc "\<cx16\.lkupla\>"
syn match prog8BuiltInFunc "\<cx16\.lkupsa\>"
syn match prog8BuiltInFunc "\<cx16\.screen_set_mode\>"
syn match prog8BuiltInFunc "\<cx16\.screen_set_charset\>"
syn match prog8BuiltInFunc "\<cx16\.pfkey\>"
syn match prog8BuiltInFunc "\<cx16\.jsrfar\>"
syn match prog8BuiltInFunc "\<cx16\.fetch\>"
syn match prog8BuiltInFunc "\<cx16\.stash\>"
syn match prog8BuiltInFunc "\<cx16\.cmpare\>"
syn match prog8BuiltInFunc "\<cx16\.primm\>"
syn match prog8BuiltInFunc "\<cx16\.macptr\>"
syn match prog8BuiltInFunc "\<cx16\.enter_basic\>"
syn match prog8BuiltInFunc "\<cx16\.mouse_config\>"
syn match prog8BuiltInFunc "\<cx16\.mouse_get\>"
syn match prog8BuiltInFunc "\<cx16\.mouse_scan\>"
syn match prog8BuiltInFunc "\<cx16\.joystick_scan\>"
syn match prog8BuiltInFunc "\<cx16\.joystick_get\>"
syn match prog8BuiltInFunc "\<cx16\.clock_set_date_time\>"
syn match prog8BuiltInFunc "\<cx16\.clock_get_date_time\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_init\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_clear\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_set_window\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_set_colors\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_draw_line\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_draw_rect\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_move_rect\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_draw_oval\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_draw_image\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_set_font\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_get_char_size\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_put_char\>"
syn match prog8BuiltInFunc "\<cx16\.GRAPH_put_next_char\>"
syn match prog8BuiltInFunc "\<cx16\.FB_init\>"
syn match prog8BuiltInFunc "\<cx16\.FB_get_info\>"
syn match prog8BuiltInFunc "\<cx16\.FB_set_palette\>"
syn match prog8BuiltInFunc "\<cx16\.FB_cursor_position\>"
syn match prog8BuiltInFunc "\<cx16\.FB_cursor_position2\>"
syn match prog8BuiltInFunc "\<cx16\.FB_cursor_next_line\>"
syn match prog8BuiltInFunc "\<cx16\.FB_get_pixel\>"
syn match prog8BuiltInFunc "\<cx16\.FB_get_pixels\>"
syn match prog8BuiltInFunc "\<cx16\.FB_set_pixel\>"
syn match prog8BuiltInFunc "\<cx16\.FB_set_pixels\>"
syn match prog8BuiltInFunc "\<cx16\.FB_set_8_pixels\>"
syn match prog8BuiltInFunc "\<cx16\.FB_set_8_pixels_opaque\>"
syn match prog8BuiltInFunc "\<cx16\.FB_fill_pixels\>"
syn match prog8BuiltInFunc "\<cx16\.FB_filter_pixels\>"
syn match prog8BuiltInFunc "\<cx16\.FB_move_pixels\>"
syn match prog8BuiltInFunc "\<cx16\.sprite_set_image\>"
syn match prog8BuiltInFunc "\<cx16\.sprite_set_position\>"
syn match prog8BuiltInFunc "\<cx16\.memory_fill\>"
syn match prog8BuiltInFunc "\<cx16\.memory_copy\>"
syn match prog8BuiltInFunc "\<cx16\.memory_crc\>"
syn match prog8BuiltInFunc "\<cx16\.memory_decompress\>"
syn match prog8BuiltInFunc "\<cx16\.console_init\>"
syn match prog8BuiltInFunc "\<cx16\.console_put_char\>"
syn match prog8BuiltInFunc "\<cx16\.console_get_char\>"
syn match prog8BuiltInFunc "\<cx16\.console_put_image\>"
syn match prog8BuiltInFunc "\<cx16\.console_set_paging_message\>"
syn match prog8BuiltInFunc "\<cx16\.kbdbuf_put\>"
syn match prog8BuiltInFunc "\<cx16\.entropy_get\>"
syn match prog8BuiltInFunc "\<cx16\.monitor\>"
syn match prog8BuiltInFunc "\<cx16\.rombank\>"
syn match prog8BuiltInFunc "\<cx16\.rambank\>"
syn match prog8BuiltInFunc "\<cx16\.vpeek\>"
syn match prog8BuiltInFunc "\<cx16\.vaddr\>"
syn match prog8BuiltInFunc "\<cx16\.vpoke\>"
syn match prog8BuiltInFunc "\<cx16\.vpoke_or\>"
syn match prog8BuiltInFunc "\<cx16\.vpoke_and\>"
syn match prog8BuiltInFunc "\<cx16\.vpoke_xor\>"
syn match prog8BuiltInFunc "\<cx16\.vload\>"
syn match prog8BuiltInFunc "\<cx16\.FB_set_pixels_from_buf\>"
syn match prog8BuiltInFunc "\<cx16\.init_system\>"
syn match prog8BuiltInFunc "\<cx16\.init_system_phase2\>"
syn match prog8BuiltInFunc "\<cx16\.set_irq\>"
syn match prog8BuiltInFunc "\<cx16\.restore_irq\>"
syn match prog8BuiltInFunc "\<cx16\.set_rasterirq\>"
syn match prog8BuiltInFunc "\<cx16\.set_rasterline\>"
syn match prog8BuiltInFunc "\<sys\.reset_system\>"
syn match prog8BuiltInFunc "\<sys\.wait\>"
syn match prog8BuiltInFunc "\<sys\.memcopy\>"
syn match prog8BuiltInFunc "\<sys\.memset\>"
syn match prog8BuiltInFunc "\<sys\.memsetw\>"
syn match prog8BuiltInFunc "\<sys\.rsave\>"
syn match prog8BuiltInFunc "\<sys\.rrestore\>"
syn match prog8BuiltInFunc "\<sys\.read_flags\>"
syn match prog8BuiltInFunc "\<sys\.clear_carry\>"
syn match prog8BuiltInFunc "\<sys\.set_carry\>"
syn match prog8BuiltInFunc "\<sys\.clear_irqd\>"
syn match prog8BuiltInFunc "\<sys\.set_irqd\>"
syn match prog8BuiltInFunc "\<sys\.exit\>"
syn match prog8BuiltInFunc "\<sys\.progend\>"


" cx16/textio.p8
syn match prog8BuiltInVar "\<txt\.DEFAULT_WIDTH\>"
syn match prog8BuiltInVar "\<txt\.DEFAULT_HEIGHT\>"
syn match prog8BuiltInFunc "\<txt\.clear_screen\>"
syn match prog8BuiltInFunc "\<txt\.home\>"
syn match prog8BuiltInFunc "\<txt\.nl\>"
syn match prog8BuiltInFunc "\<txt\.spc\>"
syn match prog8BuiltInFunc "\<txt\.column\>"
syn match prog8BuiltInFunc "\<txt\.fill_screen\>"
syn match prog8BuiltInFunc "\<txt\.clear_screenchars\>"
syn match prog8BuiltInFunc "\<txt\.clear_screencolors\>"
syn match prog8BuiltInVar "\<txt\.color_to_charcode\>"
syn match prog8BuiltInFunc "\<txt\.color\>"
syn match prog8BuiltInFunc "\<txt\.color2\>"
syn match prog8BuiltInFunc "\<txt\.lowercase\>"
syn match prog8BuiltInFunc "\<txt\.uppercase\>"
syn match prog8BuiltInFunc "\<txt\.scroll_left\>"
syn match prog8BuiltInFunc "\<txt\.scroll_right\>"
syn match prog8BuiltInFunc "\<txt\.scroll_up\>"
syn match prog8BuiltInFunc "\<txt\.scroll_down\>"
syn match prog8BuiltInFunc "\<txt\.chrout\>"
syn match prog8BuiltInFunc "\<txt\.print\>"
syn match prog8BuiltInFunc "\<txt\.print_ub0\>"
syn match prog8BuiltInFunc "\<txt\.print_ub\>"
syn match prog8BuiltInFunc "\<txt\.print_b\>"
syn match prog8BuiltInFunc "\<txt\.print_ubhex\>"
syn match prog8BuiltInFunc "\<txt\.print_ubbin\>"
syn match prog8BuiltInFunc "\<txt\.print_uwbin\>"
syn match prog8BuiltInFunc "\<txt\.print_uwhex\>"
syn match prog8BuiltInFunc "\<txt\.print_uw0\>"
syn match prog8BuiltInFunc "\<txt\.print_uw\>"
syn match prog8BuiltInFunc "\<txt\.print_w\>"
syn match prog8BuiltInFunc "\<txt\.input_chars\>"
syn match prog8BuiltInFunc "\<txt\.setchr\>"
syn match prog8BuiltInFunc "\<txt\.getchr\>"
syn match prog8BuiltInFunc "\<txt\.setclr\>"
syn match prog8BuiltInFunc "\<txt\.getclr\>"
syn match prog8BuiltInFunc "\<txt\.setcc\>"
syn match prog8BuiltInFunc "\<txt\.setcc2\>"
syn match prog8BuiltInFunc "\<txt\.plot\>"
syn match prog8BuiltInFunc "\<txt\.width\>"
syn match prog8BuiltInFunc "\<txt\.height\>"


" conv.p8
syn match prog8BuiltInFunc "\<conv\.str_ub0\>"
syn match prog8BuiltInFunc "\<conv\.str_ub\>"
syn match prog8BuiltInFunc "\<conv\.str_b\>"
syn match prog8BuiltInFunc "\<conv\.str_ubhex\>"
syn match prog8BuiltInFunc "\<conv\.str_ubbin\>"
syn match prog8BuiltInFunc "\<conv\.str_uwbin\>"
syn match prog8BuiltInFunc "\<conv\.str_uwhex\>"
syn match prog8BuiltInFunc "\<conv\.str_uw\>"
syn match prog8BuiltInFunc "\<conv\.str_w\>"
syn match prog8BuiltInFunc "\<conv\.any2uword\>"
syn match prog8BuiltInFunc "\<conv\.str2ubyte\>"
syn match prog8BuiltInFunc "\<conv\.str2byte\>"
syn match prog8BuiltInFunc "\<conv\.str2uword\>"
syn match prog8BuiltInFunc "\<conv\.str2word\>"
syn match prog8BuiltInFunc "\<conv\.hex2uword\>"
syn match prog8BuiltInFunc "\<conv\.bin2uword\>"
syn match prog8BuiltInFunc "\<conv\.ubyte2decimal\>"
syn match prog8BuiltInFunc "\<conv\.uword2decimal\>"
syn match prog8BuiltInFunc "\<conv\.byte2decimal\>"
syn match prog8BuiltInFunc "\<conv\.ubyte2hex\>"
syn match prog8BuiltInFunc "\<conv\.uword2hex\>"


" cx16logo.p8
syn match prog8BuiltInFunc "\<cx16logo\.logo_at\>"
syn match prog8BuiltInFunc "\<cx16logo\.logo\>"
syn match prog8BuiltInVar "\<cx16logo\.logo_lines\>"


" diskio.p8
syn match prog8BuiltInFunc "\<diskio\.directory\>"
syn match prog8BuiltInFunc "\<diskio\.list_files\>"
syn match prog8BuiltInFunc "\<diskio\.lf_start_list\>"
syn match prog8BuiltInFunc "\<diskio\.lf_next_entry\>"
syn match prog8BuiltInFunc "\<diskio\.lf_end_list\>"
syn match prog8BuiltInFunc "\<diskio\.f_open\>"
syn match prog8BuiltInFunc "\<diskio\.f_read\>"
syn match prog8BuiltInFunc "\<diskio\.f_read_all\>"
syn match prog8BuiltInFunc "\<diskio\.f_readline\>"
syn match prog8BuiltInFunc "\<diskio\.f_close\>"
syn match prog8BuiltInFunc "\<diskio\.status\>"
syn match prog8BuiltInFunc "\<diskio\.save\>"
syn match prog8BuiltInFunc "\<diskio\.load\>"
syn match prog8BuiltInFunc "\<diskio\.delete\>"
syn match prog8BuiltInFunc "\<diskio\.rename\>"


" prog8_lib.p8
syn match prog8BuiltInFunc "\<prog8_lib\.pattern_match\>"


" string.p8
syn match prog8BuiltInFunc "\<string\.length\>"
syn match prog8BuiltInFunc "\<string\.left\>"
syn match prog8BuiltInFunc "\<string\.right\>"
syn match prog8BuiltInFunc "\<string\.slice\>"
syn match prog8BuiltInFunc "\<string\.find\>"
syn match prog8BuiltInFunc "\<string\.copy\>"
syn match prog8BuiltInFunc "\<string\.compare\>"
syn match prog8BuiltInFunc "\<string\.lower\>"
syn match prog8BuiltInFunc "\<string\.upper\>"


" test_stack.p8
syn match prog8BuiltInFunc "\<test_stack\.test\>"


hi def link prog8BuiltInVar Identifier
hi def link prog8BuiltInFunc Function