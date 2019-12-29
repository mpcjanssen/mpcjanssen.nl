{:title "Building your own OS"
 :layout :post
 :tags  ["dev"]
 :toc true}

If you want to make your own OS and you like all clear cut code which
compiles and does everything you would have ever wanted well then look
further (go see Linux). But if you want to build your own OS including
booting, switching to PM etcetera etcetera, then this is the place to
be!

Recently I started my own attempt at writing an OS, well you can't
exactly call it that right now, it only boots from a floppy and switches
to Protected Mode right now, but that was hard enough to figure out.

Although there is a lot of info on the net concerning PM, boot sectors,
BIOS and all, I have yet to find a place where all this stuff is treated
step by step (and explained as well).

The following sections are an attempt to guide you through the pitfalls
(especially switching to Protected Mode) of booting from a floppy drive,
switching to protected mode and loading a kernel (a very simple one).

As my own project will progress I will try to take you along with me. So
the order in which subjects are treated is probably not the most logical
or most educational but it happens to be the order in which I stumbled
(or better fell and broke a leg :-)) across things.

I hope this will help you in finding the right path (a well at least a
path) through the slippery stuff called OS-programming and that you will
have fun doing it. If you have any comments, suggestions or burning
questions or if you want to point out some terrible mistakes I made,
please feel free to contact me.

## Things you should know or have to get started

It is assumed you have a Pentium computer at your disposal (though a
lot of stuff will work on a 386 as well). Further I use The Netwide
Assembler, [NASM](http://www.nasm.us/) for short, because it's ease of
use (much more logical than MASM or TASM in my humble opinion), because
everybody else uses it and because it is FREE (well you're from Holland
or you're not :-)). Last but not least you have to be a little insane
;-) (Thanx to Raphael Gray for pointing out this very important
prerequisite) Further more I assume you have the best debugger of all
time, debug.exe (included in windows and dos) "installed" on your
system. Well guess that's all so let's get dirty. And start with that
magical process called booting.

## Booting the PC and the boot sector

When you switch on your computer, program execution starts at memory
location `F000:FFF0`. (this is a real mode address, if you don't get that
right now, don't worry I will get to that when I discuss Protected Mode,
and it isn't that important anyway.) This memory location is part of the
ROM-BIOS (Basic Input/Output System) which is installed in all IBM
compatibles. The computer then starts executing a routine called the
POST (Power On Self Test) which checks if a keyboard is plugged in, if
the CMOS OK and that kind of stuff. Not very interesting from an
OS-programmer's point of view. But the last action the POST takes is
looking for a bootable disk. Hey this gets more interesting!

The BIOS checks your drives in the order which is defined in your
BIOS-SETUP. (when we start testing some boot disks we'll be booting from
a floppy drive, so make sure your BIOS checks your flops first) But how
does it know that a disk is bootable? Simple a bootable disk has the
signature 0AA55h (h=hexadecimal) in the last two bytes (that is 55h in
the second last and AAh in the last byte because of the Endian byte
ordering in the x86 family). If this signature is found, sector 0 (the
boot sector which is 200h=512 bytes big) is loaded in memory at location
0000:7C00h and control is transferred to this address. Well that wasn't
so hard, was it ? So lets try to write or own boot sector which only
displays:

``` 
NO BOOT DISK
```

and then totally freezes. (to keep it simple) Here we go:

``` nasm
;------------------------------------------------------------------------------------------ 
; dumbboot.asm                                  
; demonstrates getting control after the compu has booted   
; does nothing but display "NO BOOT DISK"  and freeze       
;                                   
; compile with NASM to binary file (nasm is assumed to be in your path)     
;  nasm dumbboot.asm                    
;                                           
; written by emJay (c) 1998 last updated 29-08-98                   
;                                           
;------------------------------------------------------------------------------------------ 

   org 07C00h                      ;this tells nasm that the first byte will be positioned  
                                   ;at address 0000:07C00h so that all the jumps will
                                   ;be correct

   jmp short begin_bootroutine     ;jump to the start of our bootroutine skipping any data

BootMessage: db "NO BOOT DISK"     ;reserve space for the bootmessage and initialize it
MessageLength equ $-BootMessage    ;calculate the length of the boot message at compile time

begin_bootroutine:                 ;this is where the bootroutine starts

   mov ax, 0
   mov es, ax                      ;point es to the segment of the boot message
   mov cx, MessageLength

   mov ax,01301h                   ;Function 13h (ax=13h) Attribute in bl (al = 01h)
   mov bx,0007h                    ;screen page 0 (bh=0)  white on black (bl=07h)
   mov dx,0                        ;start in left corner
   mov bp, BootMessage             ;takes offset of BootMessage (no offset keyword, GREAT!)
   int 10h                         ;display the string

   spin: jmp short  spin           ;go into an infinite loop

   times 510-($-$$) db 0           ;fill with zeroes until byte 510 of the boot sector
                                   ;See NASM doc for more info on $ and $$)
   dw 0xAA55                       ;write boot signature (actually goes in memory as 55h AAh)
```

Got that? Well lets compile it using [NASM](http://www.nasm.us/)

> nasm dumbboot.asm

This gives a file called "dumbboot" which an exact binary picture so no
headers. Now type (assuming you use A:)

> debug dumbboot - w 100 0 0 1 - q

This writes your homemade bootsector to A:. (to use B: type w 100 1 0 1)
Now reboot your PC and TADA we've grabbed control, isn't that great?

## The DOS boot sector

The previous boot sector has one big problem. DOS won't recognize the
disk anymore. So if we would want to put a new bootsector at the disk,
debug gives a write error, because it hasn't got a clue wat type of disk
it is.

Well, how does DOS know what kind of disk is inserted? Very simple, on
the bootsector there are some reserved fields which tell DOS what kind
of disk it is. And because our previous bootsector uses those fields to
store code, DOS can't handle the disk anymore. So let's fix that! The
fields in the bootsector are defined as follows:

### DOS boot sector layout

```
>   Offset     Field description                                                      Length of field
>   ---------- ---------------------------------------------------------------------- -----------------
>   00h        Short (JMP xx , NOP) or long (JMP xxx) jump to begin of boot routine   3 bytes
>   03h        OEM identification                                                     8 bytes
>   0Bh        Bytes per sector                                                       1 word
>   0Dh        Sectors per cluster                                                    1 byte
>   0Eh        Number of reserved sectors                                             1 word
>   10h        Number of FATs                                                         1 byte
>   11h        Number of entries in root directory                                    1 word
>   13h        Number of sectors in volume                                            1 word
>   15h        Media descriptor                                                       1 byte
>   16h        Number of sectors per FAT                                              1 word
>   18h        Sectors per track                                                      1 word
>   1Ah        Number of read/write heads                                             1 word
>   1Ch        Number of hidden sectors                                               1 word
>   1Eh-1FDh   Boot routine                                                           480 bytes
>   1FEh       55h                                                                    1 byte
>   1FFh       AAh                                                                    1 byte
>
```

OK that seems pretty straightforward, so let's try to put it to
practice. In the following piece of code I am assuming you use a 3,5" HD
disk.

```nasm
;------------------------------------------------------------------------------------------
; dosboot.asm
; demonstrates getting control after the compu has booted
; does nothing but display "NO BOOT DISK"  and freeze
; while DOS is still able to read/write the disk
;
; compile with NASM to binary file (nasm is assumed to be in your path)
;  nasm dosboot.asm
;
; written by emJay (c) 1998 last updated 31-08-98
;
;------------------------------------------------------------------------------------------         
   org 07C00h                      ;this tells nasm that the first byte will be positioned
                                   ;at address 0000:07C00h so that all the jumps will
                                   ;be correct

   jmp short begin_bootroutine     ;jump to the start of our bootroutine skipping any data
   nop                             ;first field must be 3 bytes long jmp short is 2 bytes
   db 'MajOS1.0'                   ;OEM identification
   dw 512                          ;Bytes per sector
   db 1                            ;Sectors per cluster
   dw 1                            ;Number of reserved sectors
   db 2                            ;Number of FATs
   dw 0E0h                         ;Number of dirs in root
   dw 0B40h                        ;Number of sectors in volume
   db 0F0h                         ;Media descriptor
   dw 9                            ;Number of sectors per FAT
   dw 18                           ;Number of sectors per track
   dw 2                            ;Number of read/write heads
   dw 0                            ;Number of hidden sectors

begin_bootroutine:                 ;this is where the bootroutine starts

   mov ax, 0
   mov es, ax                      ;point es to the segment of the boot message
   mov cx, MessageLength

   mov ax,01301h                   ;Function 13h (ax=13h) Attribute in bl (al = 01h)
   mov bx,0007h                    ;screen page 0 (bh=0)  white on black (bl=07h)
   mov dx,0                        ;start in left corner
   mov bp, BootMessage             ;takes offset of BootMessage (no offset keyword, GREAT!)
   int 10h                         ;display the string

   spin: jmp short  spin           ;go into an infinite loop

   times 510-($-$$) db 0           ;fill with zeroes until byte 510 of the boot sector
                                   ;See NASM doc for more info on $ and $$)
   dw 0xAA55                       ;write boot signature (actually goes in memory as 55h AAh)
```

OK now reformat your boot disk (format a: /u) compile dosboot.asm and
write it to the bootsector of your bootdisk, just as you did before.

This disk can now again be used as a normal DOS disk, you can view it
and put files on it, but when you boot from it, it still displays NO
BOOT DISK, try it!

I hope these two sections gave you some feel of the boot process and the
boot sector. If you want more information check out Michael Tischer's
book. OK please go to the toilet, take a snack, drink some coffee and
then read on because we're going to look at Protected Mode!

## Protected Mode, what is it all about?

Although through the years most PC's have been equiped with more and
more memory, all DOS programs still had to deal with the infamous 640 KB
limit. Why wasn't it possible to access all those MB's you had installed
on your system? Because even the newest processor had to be able to
execute the 8086's code it had to operate in the same way. This means
you only had 20-bit addresses giving a total accessible memory of
$2^20 = 1MB$, even if you had 40 MB plugged in your system. On a pentium
however we have a 32-bits wide address bus which gives a theoretical
address space of $2^32 = 4GB$!

How can we access al this additional memory. Well we have to leave the
8086's real mode and switch to the incredible Protected Mode. Do you
want a codesegment of 4 GB? Do you want to put the entire Encyclopedia
Brittanica in your datasegment? Well just do it, switch to Protected
Mode (PM) and you've got access to all the memory you would ever want.

## How the PC behaves in real mode

When you reboot the PC it enters a mode known as real mode. This mode
gives maximum compatibility with the 8086 and some extra features (such
as extended registers, faster instructions ,additional instructions etc.
etc.).

In this mode memory is divided in segments of 64 KB (16 bits) with a
total addressable space of $2^20=1024KB$. Memory locations are accessed
through a segment:offset address (the so called *logical address*).
Calculation of the *physical address* (the actual byte number in memory)
is performed in the following way:

    physical address = 10h*segment+offset

For example if we take segment 9000h and offset 8000h (logical address
9000:8000h) we get physical address
`9000h*10h+8000h = 90000h + 8000h = 98000h`. (Note that this address
refers to the same physical memory location as for instance 9300:5000h
so segments overlap in real mode) To access different segments, 16-bit
segment registers (such as cs, ds and es) are used so that the maximum
address is $FFFF:000Fh = FFFFFh physical = 2^20$.

The maximal address accessable address would be FFFF:FFFFh = 10FFEFh
physical, but this can't be expressed in 20 bits. However if we find a
way to access an additional address line (the most famous A20 line) we
can even use this additional FFFF:FFFFh-FFFF:000Fh=FFF0h=65520 bytes.
(the so called High Memory Area (HMA)) But why do we have to enable this
A20 line? Why isn't it enabled at boot up?

If the A20 line would be enabled then if we got the highest 20 bit
address FFFF:000Fh = FFFFFh and we would go one byte further
(FFFF:0010h) we would access the physical address 100000h (1 0000 0000
0000 0000 0000b), however at the 8086 there is no A20 (this is the 21st
addressline because we start at A0) so that FFFF:000Fh+1= 0000:0000h
dropping the carry. Because some programs use this memory wrap feature
on the 8086, the A20 has to be disabled for complete backward
compatibility.

There is however a way to enable this A20 address line (this is what
himem.sys does on MS-DOS computers giving an additional memoryblock of
almost 64K for device drivers and so on). We can use the keyboard
controller to enable this A20 line, because the A20 line is logical
ANDed with a keyboard controller output, which is disabled at boot up.
This means that the 21st bit of an address is always: 0 AND x = 0. So
all we have to do is enable this keyboard controller output to get: 1
AND x = x. (code to do this will be presented in a later chapter)
&lt;/p&gt;

Now how can we access A31-A22 to get the 4 GB addressable memory space?
You guessed it, by switching to protected mode. However in PM, memory
management is quite a different ballplay so let's check it out.

## How the PC behaves in Protected Mode

### Segmentation in Protected Mode

In PM segmentation is performed in quite a different manner. Here a
segmentregister (CS, DS, ES FS, GS or SS) contains a *segment selector*
which is a pointer to a *segment descriptor* in the *Global or Local
Descriptor Table (GDT or LDT)*

The segment descriptor (64 bits) contains information about the segment,
like access rights, size, and base address. Let's take a look at a
segment descriptors fields


  ------- ----------------------------------------------------------------
  A       Available for use by programmer
  Base    Segment Base Address
  DB      Default operation size (0 = 16-bit segment; 1 = 32-bit segment)
  DPL     Descriptor privilege level
  G       Granularity
  Limit   Segment limit
  P       Segment present
  S       Descriptor type (0 = system; 1 = code or data)
  Type    Segment type
  ------- ----------------------------------------------------------------

Let's take a look at all those fields in a bit more detail.&lt;/p&gt;

-   A: this bit is available for your own use, for instance to create
    your own virtual memory manager.
-   Base: this is the base address of the segment. Because it's 32 bits
    long, a segment can start on any physical memory place (not just at
    64K borders as in real mode) if this field contains for instance
    5555:0000h, then this segment will start at physical
    address 55550000h. (so no multiplication with 10h as in real mode)
    However with speed in mind it is wise to let a segment start on a
    16-byte boundary.
-   DB: This field performs different functions depending on the
    segment Type. This flag is always 1 for 32-bit code and data
    segments and 0 for 16-bit code and data segments.
-   DPL: These two bits give the privilege level of the segment ranging
    from 0 (highest privilege) to 3 (lowest privilege). This flag is
    used to control access to a segment.
-   Limit: Gives the size of the segment. Although it's only 20 bits
    long, a segment can be 4 GB long this is achieved by setting the
    G bit.
-   G: If this bit is set the actual segment size is the limit times 4
    KB ($1MB * 4K = 4 GB$), if this flag is clear the size of the
    segment is the limit in bytes. So for segments bigger than 1 MB the
    size must be a mutiple of 4 KB, but this is no real restriction.
-   P: This flag indicates whether the segment is present in memory
    (set) or not present (clear). If this flag is clear the processor
    generates an segment not present exception (\#NP) when a segment
    selector that points to the segment descriptor is loaded in a
    segment register. When we are not using virtual memory or paging
    this flag is usually set.
-   S: Specifies a system segment (clear) or a code or data
    segment (set).
-   Type: Indicates the segment type (note that bits 10-8 have different
    names depending on bit 11 (code or data)) :


|  Hexadecimal | 11   | 10     | 9      | 8     | Descriptor     | Description                      |
| ------------ |----- | ------ | ------ |------ |--------------- |--------------------------------- |
|             |     | *E*  | *W*  | *A*  |               |                                 |
|  > 0        | 0   | 0    | 0    | 0    | Data          | Read-Only                       |
|  > 1        | 0   | 0    | 0    | 1    | Data          | Read-Only Accessed              |
|  > 2        | 0   | 0    | 1    | 0    | Data          | Read-Write                      |
|  > 3        | 0   | 0    | 1    | 1    | Data          | Read-Write Accessed             |
|  > 4        | 0   | 1    | 0    | 0    | Data          | Read-Only, Expand down          |
|  > 5        | 0   | 1    | 0    | 1    | Data          | Read-Only, Expand down, Accessed         |
|  > 6        | 0   | 1    | 1    | 0    | Data          | Read-Write, Expand down         |
|  > 7        | 0   | 1    | 1    | 1    | Data          | Read-Write, Expand down, Accessed       |
|             |     | *C*  | *R*  | *A*  |               |                                 |
|  > 8        | 1   | 0    | 0    | 0    | Code          | Execute-Only                    |
|  > 9        | 1   | 0    | 0    | 1    | Code          | Execute-Only, accessed          |
|  > A        | 1   | 0    | 1    | 0    | Code          | Execute/Read                    |
|  > B        | 1   | 0    | 1    | 1    | Code          | Execute/Read,accessed           |
|  > C        | 1   | 1    | 0    | 0    | Code          | Execute-Only, conforming        |
|  > D        | 1   | 1    | 0    | 1    | Code          | Execute-Only, conforming, accessed      |
|  > E        | 1   | 1    | 1    | 0    | Code          | Execute/Read-Only, conforming   |
|  > F        | 1   | 1    | 1    | 1    | Code          | Execute/Read-Only, conforming, accessed |

Because we would like to access a number of segments, we will need a lot
of segment descriptors (especially in a multi-tasking Operating System).
Therefore we make a table of segment descriptors know as the Global
Discriptor Table.

### The Global Descriptor Table

The global descriptor table (GDT) is a part of the memory where segment
descriptors are defined. The first descriptor is located at the memory
location which is loaded in the &lt;b&gt;Global Descriptor Table
Register (GDTR)&lt;/b&gt;, this is a 48-bit register containing the
address of the GDT (32 bits) and the length of the GDT in bytes (16
bits) so there can be 2&lt;sup&gt;16&lt;/sup&gt; / 8 = 8192 descriptors
in the GDT. The first descriptor in the GDT must be the so called
&lt;b&gt;null descriptor&lt;/b&gt;. This descriptor consists only of
zeroes. And although this isn't used by the system, it can be loaded to
any data-segment register (DS, ES, FS and GS) without generating an
exception. Let's look at an example of a GDT:

Let's look at the *Basic Flat Model*. This means that we have two
segments of 4 GB, a code and a data segment, which completely overlap in
memory. (So it is still possible, though not advisable, to write self
modifying code):

``` .nasm
gdtr                               ;this will be loaded in the GDTR
   dw gdt_end-gdt-1                ;length of gdt
   dd gdt                          ;linear, physical address of gdt 

gdt
gdt0                               ;null descriptor 64 bits is 2
doublewords
   dd 0         
   dd 0
code_gdt                           ;code descriptor 4 GB flat segment
starting 0000:0000h 
   dw 0ffffh                       ;Limit bits 15:00
   dw 0h                           ;Base bits 15:00
   db 0h                           ;Base bits 23:16 
   db 09ah                         ;Code execute read (0Ah)  
                                   ;Present, DPL 0 , non system segment (09h)
   db 0cfh                         ;Segment limit 19:16 (0Fh) 
                                   ;4 KB granularity, 32-bit , avl = 0 (0Ch)
   db 0h                           ;Segment Base 31:24

data_gdt                           ;data descriptor 4 GB flat segment
starting 0000:0000h
   dw 0ffffh                       ;Limit bits 15:00
   dw 0h                           ;Base bits 15:00
   db 0h                           ;Base bits 23:16 
   db 092h                         ;Data read/write (02h)
                                   ;Present, DPL 0, non system segment (09h)    
   db 0cfh                         ;Segment limit 19:16 (0Fh)
                                   ;4 KB granularity, 32 bit ,avl = 0 (0Ch)
   db 0h                           ;Segment Base 31:24

videosel                           ;simple way to write to video memory 
   dw 3999                         ;Limit 80*25*2-1 (80*25 chars + attributes) 
   dw 0x8000    
   db 0x0B                         ;Base 0xB8000  
                                   ;(in real mode segment 0B800h = 10h*0B800h = 0B8000h) 
   db 0x92                         ;Data read/write (02h)
                                   ;Present, DPL 0, non system segment (09h) 
   db 0                            ;Segment limit 19:16 (0h)
                                   ;byte-granular, 16-bit
   db 0                            ;Segment Base 31:24
gdt_end
```

Now we have seen how to set up code and data segments it would be nice
to see how we can access these segments, this is done by loading segment
selectors in segment register. &lt;a name="select"&gt;

### Segment Selectors

A segment selector is a 16-bit value used to select a segment in the
GDT. First let's take a look at the segment selector's format:

&lt;/p&gt;&lt;center&gt; &lt;table border="1"&gt;
&lt;tbody&gt;&lt;tr&gt; &lt;td align="CENTER" colspan="2"&gt;
&lt;b&gt;Segment selector overview&lt;/b&gt; &lt;/td&gt; &lt;/tr&gt;
&lt;tr&gt; &lt;td align="CENTER" colspan="2"&gt; &lt;pre&gt; 16 3 2 1 0
---------------------------------------- | Index | T | RPL |
&lt;/p&gt;&lt;ul&gt; &lt;li&gt;Index: this is the index of the segment
to be used in the GDT or LDT. In our previous example of a GDT, the null
selector would have an index of 0h, the code segment selector an index
of 1h and so on. I guess this is the actual reason why there can only be
8192 selectora. (the index field is 13 bits wide and
2&lt;sup&gt;13&lt;/sup&gt; = 8192 = 2000h) &lt;/li&gt;&lt;li&gt;TI: this
tells the processor whether the descriptor should be taken out of de GDT
or the LDT (Local Descriptor Table, this table can be defined for every
seperate process in a multitasking environment). In our case TI = 0 so
that we'll use the GDT. &lt;/li&gt;&lt;li&gt;RPL: The requested
privilege level must be smaller or equal to the descriptor privilege
level (so higher or same priority) to be able to access the segment. If
this is not the case a general protection exception will be generated
(\#GP). In our case we'll use RPL = 0. &lt;/li&gt;&lt;/ul&gt;

Assume we would want to access the datasegment from the GDT, with RPL =
0. We would then have to load for example DS with 10h (0000 0000 0000
1000b). If we now want to place a white on black 'a' (character code
041h color attribute 07h) in the first place of the video memory we
could say:&lt;/p&gt;

mov word \[0xB8000\],0x0741&lt;/p&gt;

We could also load for instance gs with 18h (selecting the videosegment)
and say:&lt;/p&gt;

mov word \[gs:0\],0x741 ;remember segment-base = 0xB8000 so offset =
0h&lt;/p&gt;

Now the only thing left mentioning is how to set up the GDTR. Well
luckely there is a special instruction which does this for us:
&lt;b&gt;lgdt (Load Global descriptor table)&lt;/b&gt;. The limit loaded
in the GDTR is an offset to the last valid byte, so a limit of 0 results
in exactly one valid byte. So if we would want to load the GDTR in our
case the limit would be gdt\_end-gdt-1, because the label gdt\_end is
one byte after the last byte of the GDT, which is exactly what I've put
at label gdtr. The base address of our GDT will be 0000:16-bit offset of
gdt, or simpler just gdt. Again I have put that there. So all we have to
do is load the GDTR with the value specified at gdt:&lt;/p&gt;

> o32 lgdt \[gdtr\]&lt;/p&gt;

o32 is a NASM keyword which tells the assembler that our operator size
prefix is 32-bit, I don't know whether this is absolutely necessary.
(any suggestions?) &lt;/p&gt;

This is all we need to know about memory access in PM for the moment.
Now the time has come to do the actual switch.

&lt;/p&gt;&lt;center&gt;&lt;h2&gt;8. Switching from real to Protected
Mode&lt;/h2&gt;&lt;/center&gt;&lt;b&gt;The operation mode of the
processor is controlled by the least significant bit of the 32-bit
control register 0 (CR0), also called the protection enable (PE)
bit.&lt;/b&gt; Because it's paramount to leave the other bits unchanged
this is done in the following way:

&lt;/p&gt;&lt;pre&gt;mov eax,cr0 ;load eax with the contents of cr0 or
eax,1 ;set the least significant bit leave the other bits unchanged mov
cr0,eax ;switch to PM &lt;/pre&gt;

Before switching to PM, there are a few things you need to do:
&lt;/p&gt;&lt;ol&gt; &lt;li&gt;cli: Disable interrupts, because the
installed interrupts are all written for real mode and if an interrupt
would occur after the mode switch, your system would probably reboot.
&lt;/li&gt;&lt;li&gt;Load the GDTR using lgdt, to set up the GDT.
&lt;/li&gt;&lt;li&gt;Execute a mov CR0 instruction to set the PE bit of
control register 0. &lt;/li&gt;&lt;li&gt;Immediately after the mov,cr0
instruction perform a far jump to clear the instruction prefetch queue,
because it's still filled with real mode instructions and addresses.
&lt;/li&gt;&lt;li&gt;Reload all the segment registers except CS. (which
is reloaded by the far jump) &lt;/li&gt;&lt;li&gt; Load the Interrupt
descriptor tables to make interrupts possible &lt;/li&gt;&lt;li&gt;sti:
Re-enable interrupts. &lt;/li&gt;&lt;li&gt;Enable the A20 line to
prevent memorywrap. &lt;/li&gt;&lt;/ol&gt;

In the following source, I am only going to load the GDT and switch to
PM. So I will not set up a stack or an IDT, which is fine as long as you
don't POP or PUSH and leave interrupts disabled. When you boot this
example the following actions will be taken:&lt;/p&gt;

&lt;/p&gt;&lt;ol&gt; &lt;li&gt;The screen will be erased.
&lt;/li&gt;&lt;li&gt;A brown 'a' will be printed in the left corner of
the screen. &lt;/li&gt;&lt;li&gt;The system will wait for a keypress.
&lt;/li&gt;&lt;li&gt;The switch to PM will be made.
&lt;/li&gt;&lt;li&gt;A white 'a' will be printed in the left corner of
the screen. &lt;/li&gt;&lt;li&gt;The system will go into an infinite
loop (note that CTRL+ALT+DEL will no longer function, because interrupts
are still disabled). &lt;/li&gt;&lt;/ol&gt;

&lt;a
href="<http://web.archive.org/web/20010424064833/http://www.phys.uu.nl/~mjanssen/osdev/pmboot.asm>"&gt;Download
pmboot.asm&lt;/a&gt;&lt;/p&gt;

&lt;/p&gt;&lt;center&gt;&lt;h2&gt;9. Enable the A20 address
line&lt;/h2&gt;&lt;/center&gt;In order to use the full amount of RAM
plugged in your computer you have to enable the a20 addressline. As
mentioned earlier this can be done by enabling a line of the floppy
controller. The state of this line can be changed by setting the
appropriate bit. This bit is the second bit of the AT keyboard
controller output port. (port 064h) So in theory we can enable the a20
address line by simply setting this second bit.

There are however some things to be taken into account. The keyboard
buffer (that is the buffer on the keyboard, not the BIOS-buffer) can
still contain some bytes which have to be handled first. &lt;/p&gt;

If we have completly cleared the keyboard buffer we try to set the a20
line. This should then enable us to use the additional 64K HMA. So we
can test whether the a20 gate is enabled by writing a byte to
FFFF:000Fh+1 and check whether this byte is different from the one at
0000:0001h. Because if a20 is enabled FFFF:000Fh+1=100000h physical and
if a20 is not enabled a wrap will occur thus writing a byte to 000000h
physical. &lt;/p&gt;

To be able to see if the byte positioned at the physical address 00000h
has really changed we try to write the bit inverted (by using NOT) byte
of the original value of 00000h. In that manner it's always possible to
see if 00000h has changed (which would imply that a20 is not enabled).
&lt;/p&gt;

The code I have used below is not written by me. (although I have added
some comments) I think Tran originally wrote this code for use in his
PMode protected mode wrapper. The piece of code conains a function
EnableA20 which should do exactly that. So here we go: &lt;/p&gt;

``` .nasm
enablea20kbwait:                      ;wait for safe to write to 8042
   xor cx,cx                          ;loop a maximum of FFFFh times
enablea20kbwaitl0:
   jmp short $+2                      ;these three jumps are inserted to
wait some clockcycles
   jmp short $+2                      ;for the port to settle down
   jmp short $+2
   in al,64h                          ;read 8042 status
   test al,2                          ;buffer full? zero-flag is set if
bit 2 of 64h is not set
   loopnz enablea20kbwaitl0           ;if yes (bit 2 of 64h is set), loop
until cx=0
  ret
```

> ;while the above loop is executing keyboard interrupts will occur
> which will empty the buffer ;so be sure to have interrupts still
> enabled when you execute this code
>
> enablea20test: ;test for enabled A20
>
> :   mov al,byte \[fs:0\] ;get byte from 0:0 mov ah,al ;preserve old
>     byte not al ;modify byte xchg al,byte \[gs:10h\] ;put modified
>     byte to 0ffffh:10h ;which is either 0h or 100000h
>
> depending on the a20 state
>
> :   cmp ah,byte \[fs:0\] ;set zero if byte at 0:0 equals
>
> preserved value
>
> :   ;which means a20 is enabled
>
> mov \[gs:10h\],al ;put back old byte at 0ffffh:10h
>
> :   ret ;return, zeroflag is set if A20
>
> enabled
>
> EnableA20: ;hardware enable gate A20 (entry point of routine
>
> > xor ax,ax ;set A20 test segments 0 and 0ffffh mov fs,ax ;fs=0000h
> > dec ax mov gs,ax ;gs=0ffffh
> >
> > call enablea20test ;is A20 already enabled? jz short enablea20done
> > ;if yes (zf is set), done
>
> ;if the system is PS/2 then bit 2 of port 92h (Programmable Option
> Select) ;controls the state of the a20 gate
>
> > in al,92h ;PS/2 A20 enable or al,2 ;set bit 2 without changing the
> > rest
>
> of al
>
> :   jmp short \$+2 ;Allow port to settle down jmp short \$+2 jmp short
>     \$+2 out 92h,al ;enable bit 2 of the POS call enablea20test ;is
>     A20 enabled? jz short enablea20done ;if yes, done
>
>     call enablea20kbwait ;AT A20 enable using the 8042
>
> keyboard controller
>
> :   ;wait for buffer empty (giving zf
>
> set)
>
> :   jnz short enablea20f0 ;if failed to clear buffer jump
>
>     mov al,0d1h ;keyboard controller command 01dh
>
> (next byte written to
>
> :   out 64h,al ;60h will go to the 8042 output port
>
>     call enablea20kbwait ;clear buffer and let line settle
>
> down
>
> :   jnz short enablea20f0 ;if failed to clear buffer jump
>
>     mov al,0dfh ;write 11011111b to the 8042 output
>
> port
>
> :   ;(bit 2 is anded with A20 so we
>
> should set that one)
>
> :   out 60h,al
>
>     call enablea20kbwait ;clear buffer and let line settle
>
> down
>
> enablea20f0: ;wait for A20 to enable
>
> :   mov cx,800h ;do 800h tries
>
> enablea20l0:
>
> :   call enablea20test ;is A20 enabled? jz enablea20done ;if yes, done
>
>     in al,40h ;get current tick counter (high
>
> byte)
>
> :   jmp short \$+2 jmp short \$+2 jmp short \$+2 in al,40h ;get
>     current tick counter (low byte) mov ah,al ;save low byte of clock
>     in ah
>
> enablea20l1: ;wait a single tick
>
> :   in al,40h ;get current tick counter (high
>
> byte)
>
> :   jmp short \$+2 jmp short \$+2 jmp short \$+2 in al,40h ;get
>     current tick counter (low byte) cmp al,ah ;compare clocktick to
>     one saved in
>
> ah
>
> :   je enablea20l1 ;if equal wait a bit longer
>
>     loop enablea20l0 ;wait a bit longer to give a20 a
>
> chance to get enabled
>
> :   stc ;a20 hasn't been enabled so set
>
> carry to indicate failure
>
> :   ret ;return to caller
>
> enablea20done:
>
> :   clc ;a20 has been enabled succesfully so
>
> clear carry
>
> :   ret ;return to caller
>
As you can see it requires quite a few lines of assembly to enable the
a20 gate. This can pose a problem because a bootsector can only be a
maximum 512 bytes. (And we still have to add code to load our kernel en
place it in memory) &lt;/p&gt; In order to make some room we will remove
the layout area DOS uses to identify the disk. This forces us to write a
program by which we can write a file to the bootsector of our bootdisk.

&lt;/p&gt;&lt;center&gt;&lt;h2&gt;10. Writing a bootsector to a non-DOS
disk&lt;/h2&gt;&lt;/center&gt;In contrast to all those lucky linux-users
who have dd at their disposal, a DOS or Windows user doesn't have an
easy way of writing a binary image to a floppy if it is not recognizable
by DOS. Because our bootsector is getting a bit full I really wanted to
remove the block with diskinfo DOS uses to recognize the disk. The
problem is that it's then impossible to use debug to write the
bootsector to the floppy. So I decided to write my very own WBS (Write
BootSector).

So what has to be done to write an arbitrary file to the bootsector of a
floppy disk? First of all the bootimage has to be read from the hard
disk and stored in memory. Then the buffer containing the bootsector has
to be written to the floppy disk.&lt;/p&gt;

``` nasm
------------------------------------------------------------------------------------------
; wbs.asm Write Boot Sector ; ; writes a binary file from harddisk to
the bootsector of floppy 0 (a:) ; ; compile with NASM to binary file
(nasm is assumed to be in your path) ; nasm wbs.asm -o wbs.com ; ;
written by emJay (c) 1999 last updated 18-06-99 ;
;------------------------------------------------------------------------------------------

> org 0x100

section .text

:   jmp Main

Welcome: db "WBS Write Boot Sector v1.0 (c)1999 emJay.",10,13,'\$'
AskInfile: db "What is the location of the bootsector on your
hardisk?",10,13,":\$" ErrorOpen: db "An error has
occurred.....quiting.",10,13,'\$' OpenSuccess: db "File opened
successfully.",10,13,'\$' InitSuccess: db "Floppy initialised
successfully.",10,13,'\$' WriteSuccess: db "Bootsector written
successfully.",10,13,'\$' Counter: db 3

Main:

:   mov ah,0x09 mov dx,Welcome int 0x21 mov dx,AskInfile int 0x21 xor
    si,si

InputLoop:

:   mov ah,0x01 int 0x21 cmp al,13 je InputDone mov byte
    \[Infile+si\],al inc si jmp InputLoop

InputDone:

:   mov byte \[Infile+si\],0 mov ax,0x3d00 mov dx,Infile int 21h jc
    Error

    mov \[Handle\],ax

    mov ah,0x09 mov dx,OpenSuccess int 0x21

    mov ah,0x3f mov bx,\[Handle\] mov cx,0x200 mov dx,FileBuffer int
    0x21 mov bx,\[Handle\] mov ah,0x3e int 0x21

    xor ax,ax mov dl,0 int 0x13 jc Error mov ah,0x09 mov dx,InitSuccess
    int 0x21

loop1:

:   mov ah,0 mov dl,0 int 0x13 mov al,1 mov ah,3 mov cx,1 mov dx,0 mov
    bx,FileBuffer int 0x13 jnc WriteOK dec byte \[Counter\] jz Error jmp
    loop1

WriteOK:

:   mov ah,0x09 mov dx,WriteSuccess int 0x21

Exit:

:   mov ah,1 mov dl,0 int 0x13 mov al,ah mov ah,0x4c int 0x21

Error:

:   mov ah,0x09 mov dx,ErrorOpen int 0x21 jmp Exit

section .bss Infile: resb 80 Handle: resb 1 FileBuffer: resb 0x200

```
&lt;/pre&gt;

&lt;/p&gt;&lt;center&gt;&lt;h2&gt;11. All
sources&lt;/h2&gt;&lt;/center&gt;&lt;ul&gt; &lt;li&gt;&lt;a
href="<http://web.archive.org/web/20010424064833/http://www.phys.uu.nl/~mjanssen/osdev/dumbboot.asm>"&gt;dumbboot.asm&lt;/a&gt;
&lt;/li&gt;&lt;li&gt;&lt;a
href="<http://web.archive.org/web/20010424064833/http://www.phys.uu.nl/~mjanssen/osdev/dosboot.asm>"&gt;dosboot.asm&lt;/a&gt;
&lt;/li&gt;&lt;li&gt;&lt;a
href="<http://web.archive.org/web/20010424064833/http://www.phys.uu.nl/~mjanssen/osdev/pmboot.asm>"&gt;pmboot.asm&lt;/a&gt;
&lt;/li&gt;&lt;/ul&gt; &lt;center&gt;&lt;h2&gt;12.
Bibliography&lt;/h2&gt;&lt;/center&gt;&lt;ol type="1"&gt;
&lt;li&gt;Michael Tischer, PC Intern, ISBN 1-55755-145-6 &lt;br&gt; A
great book on all PC related stuff, it really takes you in depth on a
large number of subjects. &lt;/li&gt;&lt;li&gt;Lance Leventhal, Lance
Leventhal's 80386 programming guide, ISBN 90-6233-440-7 &lt;br&gt; The
most important parts of the intel 80386 manual, I don't know whether the
ISBN is for the English book or the Dutch translation.
&lt;/li&gt;&lt;li&gt;Intel Architecture Software Developer's Manual,
Volume 1: Basic Architecture, Volume 2: Instruction Set Reference,
Volume 3: System Programming Guide&lt;br&gt; The manual for using Intel
processors, it covers everything from registers to instruction set and
Protected Mode. These manuals are downloadable from &lt;a
href="<http://web.archive.org/web/20010424064833/http://www.intel.com/>"&gt;Intel's
web site&lt;/a&gt; (approximatly 10 MB including addenda).
&lt;/li&gt;&lt;li&gt;Ralph Brown's Interrupt List&lt;br&gt;A complete
description of all the PC's interrupts (including BIOS and DOS) and a
description of all hardware ports. A must have for every assembly
programmer. &lt;/li&gt;&lt;/ol&gt; &lt;center&gt;&lt;h2&gt;13.
Links&lt;/h2&gt;&lt;/center&gt;&lt;ol&gt; &lt;li&gt;&lt;a
href="<http://web.archive.org/web/20010424064833/http://www.webring.org/cgi-bin/webring?ring=os&list>"
target="\_top"&gt;The OS webring&lt;/a&gt;: Links to sites which are
part of the Operating System webring. It contains a lot of good links.
&lt;/li&gt;&lt;li&gt;&lt;a
href="<http://web.archive.org/web/20010424064833/http://www.intel.com/>"
target="\_top"&gt;Intel's web site&lt;/a&gt;: for all information about
Intel processors, chipsets including datasheets and manuals. It is also
possible to order a free CD-ROM with the processor manuals and a lot of
other stuff. &lt;/li&gt;&lt;li&gt;&lt;a
href="<http://web.archive.org/web/20010424064833/http://www.pobox.com/~ralf/files.html>"
target="\_top"&gt;Ralph Brown's Home Page&lt;/a&gt;: here you can
download the Ralph Brown Interrupt list which contains all known and
(unknown) interrupts and a description of their
usage.&lt;/li&gt;&lt;/ol&gt; &lt;center&gt;&lt;h2&gt;14.
Warranty&lt;/h2&gt;&lt;/center&gt;I exclude any and all implied
warranties, including warranties of merchantability and fitness for a
particular purpose. I make no warranty or representation, either express
or implied, with respect to this source code, its quality, performance,
merchantability, or fitness for a particular purpose. I shall have no
liability for special, incidental, or consequential damages arising out
of or resulting from the use or modification of this source code.

Anyway I will by no means accept warranty for any damage caused by using
information and / or sources found on this web page. So if you f\*\*k
up, kick yourself!!! &lt;/p&gt;&lt;center&gt;&lt;h2&gt;15. Who am
I&lt;/h2&gt;&lt;/center&gt;I am a twenty-four year old physics student
from Utrecht in the Netherlands. My name is emJay (AKA Mark Janssen).
Contact me at &lt;a
href="[mailto:mjanssen@phys.uu.nl](mailto:mjanssen@phys.uu.nl)"&gt;<mjanssen@phys.uu.nl>&lt;/a&gt;

&lt;center&gt;&lt;h2&gt;16. Update
history&lt;/h2&gt;&lt;/center&gt;&lt;center&gt; &lt;table
width="90%"&gt; &lt;tbody&gt;&lt;tr&gt;&lt;td&gt;28 March 2000:
&lt;/td&gt;&lt;td&gt; Added link to OS webring in the links section.
&lt;/td&gt; &lt;/tr&gt; &lt;tr&gt;&lt;td&gt;14 March 2000:
&lt;/td&gt;&lt;td&gt; Used PHP3 to make navigation between pages
possible and create the contents (Yes, it is completly automated).
&lt;/td&gt; &lt;/tr&gt; &lt;/tbody&gt;&lt;/table&gt; &lt;/center&gt;
