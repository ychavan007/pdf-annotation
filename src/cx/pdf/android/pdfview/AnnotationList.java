package cx.pdf.android.pdfview;

/**
 * PDF Annotation: List of annotations
 * View annotation list
 * Date of last change: 20.04.2012
 */
public class AnnotationList {
        private String author;
        private String subject;
        private String contents;
        private int page;
        private String moddate;
        private int id;
        
        // class constructor
        public AnnotationList (String author, String subject, String contents, int page, 
        	String moddate, int id) {
                super();
                this.author = author;
                this.subject = subject;
                this.contents = contents;
                this.page = page;
                this.moddate = moddate;
                this.id = id;
        }
        
        // get author
        public String getAuthor() {
                return author;
        }
        
        // get subject
        public String getSubject() {
                return subject;
        }

        // get contents
        public String getContents() {
                return contents;
        }

        // get page number
        public int getPage() {
            return page;
        }
        
        // get date of last modification
        public String getModdate() {
            return moddate;
        }
        
        // get annotation identifier
        public int getIdentifier() {
            return id;
        }
        
        // check if annotation list is empty
        public boolean isEmpty() {
        	// page 0 not exists, base on 1
        	if (page == 0 && id == 0) 
        		return true;
        	return false;
        }
}
