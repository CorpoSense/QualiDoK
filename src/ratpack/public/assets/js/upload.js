$(function(){
    var loading = $('#loading');
    var MINIMUM_FILE_SIZE_KB = 16458;//The number of bytes in a kilobyte.
    var inputFile = $('#input-doc')[0].files[0];

    $('#doc-upload').on('submit', function(e){
       $('#msg-warning').addClass('hidden');
       $('#msg-warning2').addClass('hidden');
        if (!$('#input-doc').val()) {
          $('#msg-warning2').addClass('hidden');
          $('#msg-warning').removeClass('hidden');
          e.preventDefault();//prevent the page from reloading
        }else if (inputFile.size< MINIMUM_FILE_SIZE_KB) {
            if(inputFile.name.split('.').pop() != "txt"){
              $('#msg-warning').addClass('hidden');
              $('#msg-warning2').removeClass('hidden');
              e.preventDefault();//prevent the page from reloading
            }
        }else{
            $('#msg-warning').addClass('hidden');
            $('#msg-warning2').addClass('hidden');
            loading.show();
            console.log('Processing...');
            setTimeout(function(){
                $(loading).append("You will be redirected...");
            }, 5000);
        }
    });


    var $fileInput = $('.file-input');
    var $droparea = $('.file-drop-area');
    var $delete = $('.item-delete');

    // Add event listeners
    $fileInput.on('dragenter focus click', function() {
        $droparea.addClass('is-active');
    });

    $fileInput.on('dragleave blur drop', function() {
        $droparea.removeClass('is-active');
    });

    $fileInput.on('change', function() {
        var filesCount = $(this)[0].files.length;
        var $textContainer = $(this).prev('.js-set-number');

        if (filesCount === 1) {
            $textContainer.text($(this).val().split('\\').pop());
        } else {
            $textContainer.text(filesCount + ' files selected');
        }
    });

    $delete.on('click', function(e) {
        e.preventDefault();
        $fileInput.val('');
        $droparea.removeClass('is-active');
        $(this).hide();
    });

});