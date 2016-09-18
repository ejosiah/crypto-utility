/**
 * save form data in session for use in the next request
 * this is required because we do not have form data available
 * to when we are streaming multipart files
 */
(function(app, $){
    $(app.document).ready(function(){
        $('form.flash').submit(function(e){
            e.preventDefault();

            var form = this;
            var flashData = {};
            $(form).find("input[data-scope='flash'], select[data-scope='flash']").each(function(i, elm){
                var name = $(elm).prop('name');
               flashData[name] = $(elm).val()
            });
            $.ajax("/flash", {data: flashData}).done(function(){
                form.submit();
            });
        })
    });
}(window, jQuery));