/* SuperGrep. fast "kg". version 1 */
/* modification history in "sg-history" */

#include <stdio.h>
#include <string.h>
#include <ctype.h>

#define MAXLINE 200

#define EMACS    "/usr/freeware/bin/emacs"
#define DATABASE "/repo/mine"

int main(int argc, char *argv[])
{
  FILE *f;
  FILE *fup;
  char c, pwd[MAXLINE], line[MAXLINE], uline[MAXLINE], lineS[50][MAXLINE], filename[MAXLINE]="", str2[MAXLINE], oldfilename[MAXLINE]="", v[MAXLINE];
  int
    AFTER=4,      /* lines before, minimum 1*/
    i,            /* loop index*/
    search,       /* search in current file?*/
    next,         /* remainning lines to print AFTER*/
    first_print,  /* print header?*/
    index,        /* index of cyclic queue*/
    current,      /* current line no.*/
    offset,       /* help var for BEFORE print*/
    last_line,    /* last printed line no.*/
	 current_index=0,
    and=0,
    view=0,
	 nand=0,
    casesens=0;   /* -c option */

  AFTER=0;

  while (--argc>0 && (*++argv)[0]=='-')
    while (c=*++argv[0])
      switch (toupper(c)) {
        case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
          AFTER=AFTER*10+c-'0';
          break;

        case 'C':
		    if (casesens) { printf("sg: not twice %c\n", c); return 0; }
          casesens=1;
          break;

        case 'A':
		    if (and || nand) { printf("sg: not twice %c\n", c); return 0; }
          and=1;
          break;

        case 'N':
		    if (nand || and) { printf("sg: not twice %c\n", c); return 0; }
          nand=1;
          break;

        case 'H':
          system("cat /projects/env/source/sg-history");
          return 0;
          break;
 
        case 'V':
		    if (view) { printf("sg: not twice %c\n", c); return 0; }
          view=1;
          break;

        default:
          printf("sg: illegal option %c\n", c);
          argc=0;
          break;
      }

  if (AFTER<=0 || AFTER>45) 
    AFTER=4;

  if (argc!=1+and+nand) {
    printf("Super Grep v1\n");
    printf("Usage: sg [-n] [-c] [-a|-n] [-v] string [string2]\n");
    printf("           -n Number of surrounded lines. max 45, default 4\n");
    printf("           -c Case sensitive search\n");
    printf("           -a search string And string2\n");
    printf("           -n search string Not string2\n");
    printf("           -v enable Viewing the results with emacs\n");
    printf("           -h show modification History\n");
    return 0;
  }

  f=  popen("gzcat "DATABASE"/filecode",  "r");
  fup=popen("gzcat "DATABASE"/filecodeup","r");

  getcwd(pwd+100, MAXLINE);
  strcpy(pwd, strrchr(pwd+100,'/')+1);
  if (!strstr(pwd, "_lib") && !strstr(pwd, "main")) 
    strcpy(pwd,".");
  else
    printf("Searching in %s\n", pwd);
      
  if (and==1) {
    strcpy(str2, *argv);
    argv++;
    if (!casesens)
      for (i=0; i<strlen(str2); i++)
        str2[i]=toupper(str2[i]);
  }

  if (nand==1) {
    strcpy(str2, *argv);
    argv++;
    if (!casesens)
      for (i=0; i<strlen(str2); i++)
        str2[i]=toupper(str2[i]);
  }
       
  if (!casesens)
    for (i=0; i<strlen(*argv); i++)
      (*argv)[i]=toupper((*argv)[i]);

  while (!feof(f)) {
    fgets( line, MAXLINE, f);
    fgets(uline, MAXLINE, fup);

    /* new header?*/

    if (line[0]=='_') {

      if (strlen(oldfilename)>0 && view && printf("\nView? ") && strncasecmp(gets(v),"y",1)==0 && fork()==0) {
        printf("Ok\n");
        strcpy(v,DATABASE);
        strcat(v,"/");
        strcat(v,oldfilename);
        if (execl(EMACS,strrchr(EMACS,'/')+1,v)<0)
          return 0;
      }
      else 
        strcpy(oldfilename, "");

      fgets( line, MAXLINE, f);
      fgets(uline, MAXLINE, fup);
      sscanf(line, "%s", filename);
      
      if (strstr(filename,pwd))
        search=1;
      else if (search==1)
	     return 0; 
      else
        search=0;
        
      next       =0;
      current    =0;
      index      =0;
      first_print=1;
      last_line  =0;
    }

    /* pattern found?*/

    else if (search) {
    
      strcpy(lineS[index++], line);
      index%=AFTER;
      current++;

      /* print AFTER lines*/

      if (next>0) {
        if (strstr(casesens?line:uline, nand?str2:*argv) && 
		  (and+nand==0 || (and==1 && strstr(casesens?line:uline, str2)) || (nand==1 && !strstr(casesens?line:uline, *argv) ))) {
          printf("*");
          next=AFTER;
        }
        else {
          printf(" ");
          next--;
        }
        printf(" %d\t%s", current, line);
        last_line=current;
      }

      /* print BEFORE lines*/

        else if (strstr(casesens?line:uline, nand?str2:*argv) && 
		  (and+nand==0 || (and==1 && strstr(casesens?line:uline, str2)) || (nand==1 && !strstr(casesens?line:uline, *argv) ))) {

        /* print header?*/

        if (first_print) {
			 current_index++;
          printf("\n%d\n%s\n\n", current_index, (strstr(pwd, "_lib") || strstr(pwd, "main"))?strstr(filename,"/")+1:filename);
          strcpy(oldfilename, filename);
          
          first_print=0;
        }

        /* in "print AFTER-1 lines"?*/

        if (next==0) {

          if (current-AFTER+1>last_line)
            printf("\n");

          i=index;
          for (offset=-(AFTER-1); offset<0; offset++) {
            if (1<=current+offset && current+offset>last_line)
              printf("  %d\t%s", current+offset, lineS[i]);
            i++;
            i%=AFTER;
          }

          printf("* %d\t%s", current, line);
          next=AFTER;
        }
      }
    }
  }

  if (strlen(oldfilename)>0 && view && printf("\nView? ") && strncasecmp(gets(v),"y",1)==0 && fork()==0) {
    printf("Ok\n");
    strcpy(v,DATABASE);
    strcat(v,"/");
    strcat(v,oldfilename);
    if (execl(EMACS,strrchr(EMACS,'/')+1,v)<0)
      return 0;
  }
  else 
    strcpy(oldfilename, "");
  
  return 0;
}
